package nl.surf.rdx.librarian

import cats.effect.{IO, IOApp}
import cats.implicits._
import natchez.Trace.Implicits.noop
import nl.surf.rdx.common.db.DbSession
import nl.surf.rdx.common.model.api.ShareInfo
import nl.surf.rdx.librarian.codecs.service.DatasetService
import nl.surf.rdx.librarian.conf.LibrarianConf
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object LibrarianApp extends IOApp.Simple {

  val run: IO[Unit] = {
    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    for {
      conf <- LibrarianConf.loadF[IO]
      share2shareInfo <- ShareInfo.fromShare[IO].run(ShareInfo.Deps(conf.conditionsFileName))
      dsService <-
        DatasetService
          .make[IO]
          .run(DatasetService.Deps[IO](DbSession.resource[IO], conf, share2shareInfo))
      _ <-
        BlazeServerBuilder[IO](ExecutionContext.global)
          .bindHttp(conf.httpPort, "0.0.0.0")
          .withHttpApp(
            (LibrarianRoutes.getDatasetRoute(dsService) <+>
              LibrarianRoutes.publishDatasetRoute(dsService) <+>
              LibrarianRoutes.getRdxShareRoute(dsService)).orNotFound
          )
          .resource
          .use(_ => IO.never)
    } yield ()

  }
}
