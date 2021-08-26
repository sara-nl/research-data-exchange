package nl.surf.rdx.sharer.owncloud

import cats.{FlatMap, Monad}
import cats.data.Kleisli
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, IO, Resource, Sync}
import com.github.sardine.{DavResource, Sardine}
import io.circe.Json
import io.circe.generic.auto._
import cats.implicits._
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.SharerApp
import nl.surf.rdx.sharer.SharerApp.{Deps, KIO}
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf.WebdavBase
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, Headers, Request, Uri}
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.ListHasAsScala

object OwncloudShares {

  def getShares[F[_]: Monad: ConcurrentEffect: Logger: FlatMap]
      : Kleisli[F, Deps, List[OwncloudShare]] =
    Kleisli {
      case Deps(conf) =>
        val httpClientR = BlazeClientBuilder[F](ExecutionContext.global)
          .withConnectTimeout(conf.client.connectionTimeout)
          .withResponseHeaderTimeout(conf.client.responseHeaderTimeout)
          .withRequestTimeout(conf.client.requestTimeout)
          .withIdleTimeout(conf.client.idleTimeout)
          .resource

        val sharesRequest = Request[F](
          uri = Uri.unsafeFromString(conf.owncloud.sharesSource),
          headers = Headers.of(
            Authorization(
              BasicCredentials(conf.owncloud.webdavUsername, conf.owncloud.webdavPassword)
            )
          )
        )

        httpClientR.use { client =>
          for {
            _ <- Logger[F].trace(s"Begin shares request")
            shares <-
              client
                .expect[Json](sharesRequest)
                .flatMap(json =>
                  for {
                    _ <- Logger[F].trace(
                      s"Extracting OC Shares from: ${json.spaces2}"
                    )
                    shares <- Sync[F].fromEither(
                      json.hcursor
                        .downField("ocs")
                        .downField("data")
                        .as[List[OwncloudShare]]
                    )
                  } yield shares
                )
            _ <- Logger[F].trace(s"End shares request")
            _ <- Logger[F].debug(
              s"Retrieved ${shares.length} shares"
            )
          } yield shares
        }
    }

  //  TODO: SardineException 502 => retry
  //  TODO: org.apache.http.NoHttpResponseException => retry
  //  TODO: javax.net.ssl.SSLException: Connection reset
  def listTopLevel[F[_]: Sync](
      userPath: String
  ): Kleisli[F, (Sardine, WebdavBase), List[DavResource]] =
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
