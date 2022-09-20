package nl.surf.rdx.librarian.routes

import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits.{catsSyntaxFlatMapOps, _}
import cats.{ApplicativeError, FlatMap, Monad}
import com.minosiants.pencil.data.Error.InvalidMailBox
import io.lemonlabs.uri.RelativeUrl
import nl.surf.rdx.common.email.{RdxEmail, RdxEmailService}
import nl.surf.rdx.common.model.access.AccessRequest
import nl.surf.rdx.common.model.api.{ShareInfo, UserMetadata}
import nl.surf.rdx.common.model.{RdxDataset, RdxShare}
import nl.surf.rdx.librarian.codecs.service.DatasetService
import nl.surf.rdx.librarian.email.DatasetAccessLinkEml
import nl.surf.rdx.librarian.email.DatasetAccessOwnerEml
import nl.surf.rdx.librarian.error.{PublicRouteError, RoutesErrorHandler}
import nl.surf.rdx.librarian.routes.extractors._
import org.http4s._
import org.http4s.circe.CirceEntityCodec
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import java.nio.file.{Path => JPath}
import java.time.OffsetDateTime
import java.util.UUID
import scala.util.Try

object LibrarianRoutes {

  // Dependencies
  case class Deps[F[_]](
      ds: DatasetService[F],
      prepareApiView: RdxShare => F[ShareInfo],
      emailService: RdxEmailService[F],
      accessLinkTpl: RdxEmail.Template[F, DatasetAccessLinkEml.Vars],
      toOwnerTpl: RdxEmail.Template[F, DatasetAccessOwnerEml.Vars],
      makePublicLink: JPath => F[String],
      downloadConditions: String => F[String]
  )

  // HttpRoutes with dependencies
  type HttpRoutesD[F[_]] = Kleisli[F, Deps[F], HttpRoutes[F]]

  // Helper types
  type DsEncoder[F[_]] = EntityEncoder[F, RdxDataset]
  type UmDecoder[F[_]] = EntityDecoder[F, UserMetadata]
  type AcDecoder[F[_]] = EntityDecoder[F, AccessRequest]

  def all[F[_]: Logger: Sync: Monad: ApplicativeError[*[_], Throwable]: FlatMap]
      : Kleisli[F, Deps[F], HttpRoutes[F]] = {
    import CirceEntityCodec._
    import RdxDataset.codecs._
    import UserMetadata.codecs._
    import io.circe.generic.auto._
    List(
      createAccessRequestRoute[F],
      getDatasetRoute[F],
      getRdxShareRoute[F],
      publishDatasetRoute[F]
    ).sequence.map(_.reduce(_ <+> _)).map(RoutesErrorHandler[F].handle)
  }

  def createAccessRequestRoute[
      F[_]: Logger: Sync: Monad: ApplicativeError[*[_], Throwable]: FlatMap: AcDecoder
  ]: HttpRoutesD[F] =
    Kleisli.fromFunction {
      case Deps(
            datasetService,
            _,
            emailService,
            accessLinkTpl,
            toOwnerTpl,
            mkPublicLink,
            downloadConditions
          ) =>
        val dsl = Http4sDsl[F]
        import dsl._
        Router("/access" -> HttpRoutes.of[F] {
          case req @ POST -> Root / NonEmptyVar(doi) =>
            import RdxEmail.Template.syntax._
            for {
              access <- req.as[AccessRequest]
              doi <- Sync[F].fromTry(RelativeUrl.parseTry(doi))
              dsOption <- datasetService.fetchOCShare(doi)
              dataset <- Sync[F].fromOption(dsOption, PublicRouteError(Status.NotFound))
              linkVars =
                DatasetAccessLinkEml
                  .Vars[F](access.name, access.email, dataset, mkPublicLink, downloadConditions)
              ownerVars =
                DatasetAccessOwnerEml
                  .Vars[F](access.name, access.email, dataset)
              _ <-
                emailService
                  .sendMany(
                    List(
                      accessLinkTpl.seal(linkVars),
                      toOwnerTpl.seal(ownerVars)
                    )
                  )
                  .adaptError {
                    case InvalidMailBox(msg) =>
                      PublicRouteError(Status(Status.BadRequest.code, msg))
                  }
              res <- Ok()
            } yield res
        })
    }

  private def getDatasetRoute[F[_]: Logger: Sync: FlatMap: DsEncoder]: HttpRoutesD[F] =
    Kleisli.fromFunction {
      case Deps(ds, _, _, _, _, _, _) =>
        val dsl = Http4sDsl[F]
        import dsl._
        Router("/dataset" -> HttpRoutes.of[F] {
          case GET -> Root / NonEmptyVar(doi) =>
            val result = for {
              doi <- Sync[F].fromTry(RelativeUrl.parseTry(doi))
              dsOption <- ds.fetchDataset(doi)
              response <- dsOption match {
                case Some(ds) => Ok(ds)
                case None     => NotFound(s"Can not find dataset $doi")
              }
            } yield response
            result.handleErrorWith(e => {
              Logger[F].error(e)(s"Cannot load dataset ${e.getMessage}") >>
                InternalServerError("Cannot load dataset details")
            })
        })
    }

  private def getRdxShareRoute[F[_]: Logger: Sync: FlatMap: DsEncoder]: HttpRoutesD[F] =
    Kleisli.fromFunction {
      case Deps(ds, prepareApiView, _, _, _, _, _) =>
        val dsl = Http4sDsl[F]
        import dsl._
        Router("/share" -> HttpRoutes.of[F] {
          case GET -> Root / NonEmptyVar(rawToken) =>
            val response = for {
              token <- Sync[F].fromTry(Try(UUID.fromString(rawToken)))
              shareInfoOpt <- ds.fetchShare(token)
              now <- Sync[F].delay(OffsetDateTime.now())
              response <- {
                shareInfoOpt match {
                  case Some(share) if share.expiresAt.isAfter(now) =>
                    import io.circe.generic.auto._
                    import org.http4s.circe.CirceEntityCodec._
                    prepareApiView(share).flatMap(Ok(_))
                  case Some(_) => Forbidden("Token expired")
                  case None    => NotFound(s"Can not find share")
                }
              }
            } yield response
            response.handleErrorWith(e => {
              Logger[F].warn(e)("Error when applying token") >>
                Forbidden("Invalid Token")
            })
        })
    }

  private def publishDatasetRoute[F[_]: Logger: Sync: FlatMap: UmDecoder]: HttpRoutesD[F] =
    Kleisli.fromFunction {
      case Deps(ds, _, _, _, _, _, _) =>
        val dsl = Http4sDsl[F]
        import dsl._
        Router("/dataset" -> HttpRoutes.of[F] {
          case req @ POST -> Root / NonEmptyVar(token) =>
            val response: F[Response[F]] = for {
              uuid <- Sync[F].fromTry(Try(UUID.fromString(token)))
              metadata <- req.as[UserMetadata]
              _ <- ds.publishShare(uuid, metadata)
            } yield Response(Status.Created)

            response.handleErrorWith(e => {
              Logger[F].error(e)("Failed to publish share") >>
                (e match {
                  case _: InvalidMessageBodyFailure => BadRequest("Invalid body")
                  case _                            => InternalServerError("Unknown error")
                })
            })
        })
    }

}
