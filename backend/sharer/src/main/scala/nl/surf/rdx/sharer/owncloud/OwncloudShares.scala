package nl.surf.rdx.sharer.owncloud

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Sync}
import cats.implicits._
import cats.{Applicative, FlatMap, Functor, Monad}
import com.github.sardine.{DavResource, Sardine}
import io.circe.generic.auto._
import io.lemonlabs.uri.Url
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.SharerApp
import nl.surf.rdx.sharer.SharerApp.Deps
import nl.surf.rdx.sharer.conf.SharerConf.ClientConf
import nl.surf.rdx.sharer.http.RdxHttpClient
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf.WebdavBase
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, Headers, Method, Request, Uri}
import org.typelevel.log4cats.Logger

import scala.jdk.CollectionConverters.ListHasAsScala
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf

object OwncloudShares {

  case class Deps(owncloudConf: OwncloudConf, clientConf: ClientConf)

  private def httpAuth[F[_]: Applicative] =
    Kleisli[F, OwncloudConf, Authorization](conf =>
      Applicative[F].pure(
        Authorization(
          BasicCredentials(conf.webdavUsername, conf.webdavPassword)
        )
      )
    )

  private def getSharesRequest[F[_]: Applicative] =
    Kleisli[F, OwncloudConf, Request[F]](conf =>
      Applicative[F].pure(
        Request[F](
          uri = Uri
            .unsafeFromString(conf.sharesSource)
            .withQueryParam("shared_with_me", "true"),
          headers = Headers.of(
            Authorization(
              BasicCredentials(conf.webdavUsername, conf.webdavPassword)
            )
          )
        )
      )
    )

  private def makePublicLinkRequest[F[_]: Monad] =
    for {
      conf <- Kleisli.ask[F, OwncloudConf]
      auth <- httpAuth[F]
      shareName = "RDX public link"
    } yield (userPath: String) =>
      Request[F](
        method = Method.POST,
        uri = Uri
          .unsafeFromString(conf.sharesSource)
          .withQueryParam("name", shareName)
          .withQueryParam("path", userPath)
          .withQueryParam("shareType", "3"), // Don't ask why "3"
        headers = Headers.of(auth)
      )

  def getShares[F[_]: Monad: ConcurrentEffect: ContextShift: Logger]
      : Kleisli[F, SharerApp.Deps, List[OwncloudShare]] =
    for {
      request <- getSharesRequest[F].local((dd: SharerApp.Deps) => dd.conf.owncloud)
      json <- RdxHttpClient.runRequest(request).local((dd: SharerApp.Deps) => dd.conf.client)
      shares <- Kleisli.liftF(
        Sync[F].fromEither(
          json.hcursor
            .downField("ocs")
            .downField("data")
            .as[List[OwncloudShare]]
        )
      )
    } yield shares

  def makePublicLink[F[_]: Monad: ConcurrentEffect: ContextShift: Logger](
      userPath: String
  ): Kleisli[F, Deps, String] =
    for {
      request <- makePublicLinkRequest[F].local((dd: Deps) => dd.owncloudConf)
      json <- RdxHttpClient.runRequest(request(userPath)).local((dd: Deps) => dd.clientConf)
      publicLink <- Kleisli.liftF(
        Sync[F].fromEither(
          json.hcursor
            .downField("ocs")
            .downField("data")
            .downField("url")
            .as[String]
        )
      )
    } yield publicLink

  //  TODO: SardineException 502 => retry
  //  TODO: org.apache.http.NoHttpResponseException => retry
  //  TODO: javax.net.ssl.SSLException: Connection reset
  def listTopLevel[F[_]: Sync](
      userPath: String
  ): Kleisli[F, (Sardine, WebdavBase), List[DavResource]] = {
    Kleisli {
      case (sardine, WebdavBase(serverUri, serverSuffix)) =>
        import io.lemonlabs.uri.typesafe.dsl.{pathPartToUrlDsl, urlToUrlDsl}
        Sync[F].delay {
          sardine
            .list(
              (serverUri / serverSuffix / userPath)
                .normalize(removeEmptyPathParts = true)
                .toStringPunycode,
              1
            )
            .asScala
            .toList
            .tail // Skip the folder/file itself
        }
    }

  }

}
