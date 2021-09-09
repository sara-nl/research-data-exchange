package nl.surf.rdx.librarian

import cats.effect.IO
import cats.implicits.catsSyntaxFlatMapOps
import io.lemonlabs.uri.RelativeUrl
import nl.surf.rdx.common.model.RdxDataset
import nl.surf.rdx.common.model.api.UserMetadata
import nl.surf.rdx.librarian.codecs.service.DatasetService
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.jsonOf
import org.http4s.dsl.io._
import org.http4s.server.Router
import org.http4s._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.OffsetDateTime
import java.util.UUID

object LibrarianRoutes {

  private val logger = Slf4jLogger.getLogger[IO]

  def getDatasetRoute(datasetService: DatasetService[IO]): HttpRoutes[IO] =
    Router("/dataset" -> HttpRoutes.of[IO] {
      case GET -> Root / doi =>
        val result = for {
          doi <- IO.fromTry(RelativeUrl.parseTry(doi))
          dsOption <- datasetService.fetchDataset(doi)
          response <- {
            dsOption match {
              case Some(ds) =>
                import RdxDataset.codecs._
                import org.http4s.circe.CirceEntityCodec._
                Ok(ds)
              case None => NotFound(s"Can not find dataset $doi")
            }
          }
        } yield response
        result.handleErrorWith(e => {
          logger.error(e)(s"Cannot load dataset ${e.getMessage}") >>
            InternalServerError("Cannot load dataset details")
        })
    })

  def getRdxShareRoute(dsServices: DatasetService[IO]): HttpRoutes[IO] =
    Router("/share" -> HttpRoutes.of[IO] {
      case GET -> Root => BadRequest()
      case GET -> Root / token =>
        val response = for {
          token <- IO(UUID.fromString(token))
          shareInfoOpt <- dsServices.fetchShare(token)
          now <- IO(OffsetDateTime.now())
          response <- {
            shareInfoOpt match {
              case Some(share) =>
                if (share.expiresAt.isAfter(now)) {
                  import io.circe.generic.auto._
                  import org.http4s.circe.CirceEntityCodec._
                  dsServices.prepareApiView(share).flatMap(Ok(_))
                } else Forbidden("Token expired")
              case None => NotFound(s"Can not find share")
            }
          }
        } yield response
        response.handleErrorWith(e => {
          logger.warn(e)("Error when applying token") >>
            Forbidden("Invalid Token")
        })
    })

  def publishDatasetRoute(
      datasetService: DatasetService[IO]
  ): HttpRoutes[IO] = {
    Router("/dataset" -> HttpRoutes.of[IO] {
      case req @ POST -> Root / token =>
        import UserMetadata.codecs._
        implicit val decoder: EntityDecoder[IO, UserMetadata] = jsonOf[IO, UserMetadata]
        val response: IO[Response[IO]] = for {
          uuid <- IO(UUID.fromString(token))
          ds <- req.as[UserMetadata]
          _ <- datasetService.publishShare(uuid, ds)
        } yield Response(Status.Created)

        response.handleErrorWith(e => {
          logger.error(e)("Failed to publish share") >> {
            e match {
              case _: InvalidMessageBodyFailure => BadRequest("Invalid body")
              case _                            => InternalServerError("Unknown error")
            }
          }
        })
    })
  }

}
