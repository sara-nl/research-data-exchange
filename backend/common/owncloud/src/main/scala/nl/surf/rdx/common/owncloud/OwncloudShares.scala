package nl.surf.rdx.common.owncloud

import cats.data.Kleisli
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Sync}
import cats.implicits._
import cats.{Applicative, FlatMap, Functor, Monad}
import fs2.{io => fs2IO}
import io.circe.generic.auto._
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.common.owncloud.conf.OwncloudConf
import nl.surf.rdx.common.owncloud.http.RdxHttpClient
import org.http4s._
import org.http4s.headers.Authorization
import org.typelevel.log4cats.Logger
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.util.UUID
import cats.implicits._
import java.nio.file.{Path => JPath}

object OwncloudShares {

  type Deps = OwncloudConf

  private def httpAuth(conf: Deps): Authorization =
    Authorization(
      BasicCredentials(conf.webdavUsername, conf.webdavPassword)
    )

  private def getSharesRequest[F[_]: Applicative] =
    Kleisli.fromFunction[F, Deps] { conf: OwncloudConf =>
      Request[F](
        uri = Uri
          .unsafeFromString(conf.sharesSource)
          .withQueryParam("shared_with_me", "true"),
        headers = Headers(httpAuth(conf))
      )
    }

  private def mkPublicLinkRequest[F[_]: Monad] =
    for {
      conf <- Kleisli.ask[F, OwncloudConf]
      shareName = "RDX public link"
    } yield (userPath: JPath) =>
      Request[F](
        method = Method.POST,
        uri = Uri
          .unsafeFromString(conf.sharesSource)
          .withQueryParam("name", shareName)
          .withQueryParam("path", userPath.toString)
          .withQueryParam("shareType", "3"), // Don't ask why "3"
        headers = Headers(httpAuth(conf))
      )

  def getShares[F[_]: Monad: ConcurrentEffect: ContextShift: Logger]
      : Kleisli[F, Deps, List[OwncloudShare]] =
    for {
      request <- getSharesRequest[F]
      runRequest <- RdxHttpClient.runRequest.local[Deps](_.client)
      json <- Kleisli.liftF(runRequest(request))
      shares <- Kleisli.liftF(
        Sync[F].fromEither(
          json.hcursor
            .downField("ocs")
            .downField("data")
            .as[List[OwncloudShare]]
        )
      )
    } yield shares

//  for local testing
//  def makePublicLink2[F[_]: Monad: Functor: FlatMap: ConcurrentEffect: ContextShift: Logger]
//      : Kleisli[F, Deps, JPath => F[String]] =
//    Kleisli.fromFunction { deps => path => Sync[F].pure("http://example.com") }

  def makePublicLink[F[_]: Monad: Functor: FlatMap: ConcurrentEffect: ContextShift: Logger]
      : Kleisli[F, Deps, JPath => F[String]] =
    for {
      mkRequest <- mkPublicLinkRequest[F]
      runRequest <- RdxHttpClient.runRequest.local[Deps](_.client)

    } yield (userPath: JPath) =>
      Logger[F].debug(s"Creating public link for ${userPath}") >>
        runRequest(mkRequest(userPath)).flatMap(json =>
          Sync[F].fromEither(
            json.hcursor
              .downField("ocs")
              .downField("data")
              .downField("url")
              .as[String]
          )
        )

//for local testing
//  def makePublicLink2[F[_]: Monad: Functor: FlatMap: ConcurrentEffect: ContextShift: Logger]
//      : Kleisli[F, Deps, String => F[String]] =
//    Kleisli.fromFunction { deps => str => Sync[F].pure("http://example.com") }

  def downloadConditions[F[_]: Sync: Applicative: ContextShift]
      : Kleisli[F, String, String => F[String]] = {
    Kleisli.fromFunction { downloadTo => url =>
      Blocker[F].use { blocker =>
        val path = s"/tmp/librarian/${UUID.randomUUID()}/$downloadTo"
        new File(path).getParentFile.mkdirs()
        fs2IO
          .readInputStream[F](
            Sync[F].delay(new URL(s"$url/download").openConnection.getInputStream),
            4096,
            blocker,
            closeAfterUse = true
          )
          .through(fs2IO.file.writeAll(Paths.get(path), blocker))
          .compile
          .drain >>
          path.pure[F]
      }
    }

  }

}
