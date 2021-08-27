package nl.surf.rdx.librarian

import cats.effect.{IO, IOApp}
import cats.implicits._
import io.lemonlabs.uri.{AbsoluteUrl, RelativeUrl}
import nl.surf.rdx.common.db.{DbSession, Shares}
import nl.surf.rdx.common.model
import nl.surf.rdx.librarian.conf.LibrarianConf
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import natchez.Trace.Implicits.noop
import nl.surf.rdx.librarian.codecs.service.DatasetService
import org.typelevel.log4cats.SelfAwareStructuredLogger

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object LibrarianApp extends IOApp.Simple {

  case class Deps(config: LibrarianConf, datasetService: DatasetService[IO])

  val run: IO[Unit] = {
    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    val demoDss = Seq(
      model.RdxDataset(
        RelativeUrl.parse("10.1000/182"),
        "Sensitive Data 001",
        "The SPSS file includes the raw data...",
        AbsoluteUrl.parse("https://circe.github.io/circe/codecs/custom-codecs.html"),
        Seq("conditions.pdf", "file1.xls", "file2.xls", "file50.xls")
      )
    )

    for {
      conf <- LibrarianConf.loadF[IO]
      dsService <- DatasetService.make[IO].run(DatasetService.Deps[IO](DbSession.resource[IO]))
      deps = Deps(conf, dsService)
      _ <-
        BlazeServerBuilder[IO](ExecutionContext.global)
          .bindHttp(conf.httpPort, "0.0.0.0")
          .withHttpApp(
            (LibrarianRoutes.getDatasetRoute(doi => demoDss.find(_.doi === doi).pure[IO])
              <+>
                LibrarianRoutes.publishDatasetRoute(dsService)
              <+> LibrarianRoutes.getUnpublishedShareRoute(deps)).orNotFound
          )
          .resource
          .use(_ => IO.never)
    } yield ()

  }
}
