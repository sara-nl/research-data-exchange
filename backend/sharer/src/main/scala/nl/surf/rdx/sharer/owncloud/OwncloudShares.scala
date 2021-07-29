package nl.surf.rdx.sharer.owncloud

import cats.data.Kleisli
import cats.effect.{ContextShift, IO, Resource}
import com.github.sardine.{DavResource, Sardine}
import io.circe.Json
import io.circe.generic.auto._
import io.lemonlabs.uri.Url
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf.WebdavBase
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, Headers, Request, Uri}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.jdk.CollectionConverters.ListHasAsScala

object OwncloudShares {

  private val logger = Slf4jLogger.getLogger[IO]

  case class Deps(
      httpClientR: Resource[IO, Client[IO]],
      creds: BasicCredentials,
      ocConf: OwncloudConf
  )

  def getShares(implicit ec: ContextShift[IO]): Kleisli[IO, Deps, List[OwncloudShare]] =
    Kleisli {
      case Deps(httpClientR, creds, rdConf) =>
        val sharesRequest = Request[IO](
          uri = Uri.unsafeFromString(rdConf.sharesSource.toString),
          headers = Headers.of(Authorization(creds))
        )

        httpClientR.use { client =>
          for {
            _ <- logger.trace(s"Begin shares request")
            shares <- client.expect[Json](sharesRequest).flatMap(extractShares)
            _ <- logger.trace(s"End shares request")
            _ <- logger.debug(
              s"Retrieved ${shares.length} shares"
            )
          } yield shares
        }
    }

  //  TODO: SardineException 502 => retry
  //  TODO: org.apache.http.NoHttpResponseException => retry
  //  TODO: javax.net.ssl.SSLException: Connection reset
  def listTopLevel(userPath: String): Kleisli[IO, (Sardine, WebdavBase), List[DavResource]] =
    Kleisli {
      case (sardine, WebdavBase(serverUri, serverSuffix)) =>
        import io.lemonlabs.uri.typesafe.dsl.{pathPartToUrlDsl, urlToUrlDsl}
        IO {
          sardine
            .list((serverUri / serverSuffix / userPath).toStringPunycode, 1)
            .asScala
            .toList
        }
    }

  private def extractShares(json: Json): IO[List[OwncloudShare]] =
    for {
      _ <- logger.trace(
        s"Extracting OC Shares from: ${json.spaces2}"
      )
      shares <- IO.fromEither(
        json.hcursor
          .downField("ocs")
          .downField("data")
          .as[List[OwncloudShare]]
      )
    } yield shares

}
