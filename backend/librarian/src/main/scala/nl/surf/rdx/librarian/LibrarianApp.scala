package nl.surf.rdx.librarian

import cats.effect.{IO, IOApp}
import io.lemonlabs.uri.{AbsoluteUrl, RelativeUrl, Url}
import nl.surf.rdx.librarian.conf.LibrarianConf
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._
import cats.implicits._
import nl.surf.rdx.common.model.ShareToken
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.URL
import java.time.{LocalDateTime, OffsetDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object LibrarianApp extends IOApp.Simple {
  val run = {

    val logger = Slf4jLogger.getLogger[IO]

    val demoDss = Seq(
      RdxDataset(
        RelativeUrl.parse("10.1000/182"),
        "Sensitive Data 001",
        "The SPSS file includes the raw data...",
        AbsoluteUrl.parse("https://circe.github.io/circe/codecs/custom-codecs.html"),
        Seq("conditions.pdf", "file1.xls", "file2.xls", "file50.xls")
      )
    )

    val demoTokens = Seq(
      ShareToken(
        OwncloudShare("id1", "uuid1", Some("sales@microsoft.com"), "/ds1", "file", 12),
        OffsetDateTime.now(),
        UUID.fromString("123e4567-e89b-12d3-a456-426614174000").some,
        OffsetDateTime.now(),
        "sales@microsoft.com",
        Nil
      )
    )

    val config = LibrarianConf(8081)

    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(config.httpPort, "0.0.0.0")
      .withHttpApp(
        LibrarianRoutes.getDatasetRoute(doi => demoDss.find(_.doi === doi).pure[IO]).orNotFound
      )
      .resource
      .use(_ => IO.never)
  }
}
