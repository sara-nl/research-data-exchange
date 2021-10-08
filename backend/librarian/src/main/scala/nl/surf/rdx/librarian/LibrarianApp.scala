package nl.surf.rdx.librarian

import cats.effect.{IO, IOApp}
import cats.implicits._
import natchez.Trace.Implicits.noop
import nl.surf.rdx.common.db.DbSession
import nl.surf.rdx.common.email.{RdxEmail, RdxEmailService}
import nl.surf.rdx.common.email.conf.EmailConf
import nl.surf.rdx.common.model.api.ShareInfo
import nl.surf.rdx.common.owncloud.OwncloudShares
import nl.surf.rdx.common.owncloud.conf.OwncloudConf
import nl.surf.rdx.librarian.codecs.service.DatasetService
import nl.surf.rdx.librarian.conf.LibrarianConf
import nl.surf.rdx.librarian.email.{DatasetAccessLinkEml, DatasetAccessOwnerEml}
import nl.surf.rdx.librarian.routes.LibrarianRoutes
import org.http4s.blaze.server._
import org.http4s.implicits._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object LibrarianApp extends IOApp.Simple {

  type Deps[F[_]] = DatasetService[F]

  val run: IO[Unit] = {
    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

    for {
      (conf, emailConf, ocConf) <-
        (LibrarianConf.loadF[IO], EmailConf.loadF[IO], OwncloudConf.loadF[IO]).parTupled
      share2shareInfo <- ShareInfo.fromShare[IO].run(ShareInfo.Deps(conf.conditionsFileName))
      sendMail <- RdxEmailService[IO]()
        .run(RdxEmailService.Deps(emailConf, RdxEmailService.Deps.clientR(emailConf)))
      mkPublicLink <- OwncloudShares.makePublicLink[IO].run(ocConf)
      downloadConditions <- OwncloudShares.downloadConditions[IO].run(conf.conditionsFileName)
      dsService <-
        DatasetService
          .make[IO]
          .run(DatasetService.Deps[IO](DbSession.resource[IO], conf))
      routes <-
        LibrarianRoutes.all
          .run(
            LibrarianRoutes
              .Deps(
                dsService,
                share2shareInfo,
                sendMail,
                DatasetAccessLinkEml[IO],
                DatasetAccessOwnerEml[IO],
                mkPublicLink,
                downloadConditions
              )
          )
      _ <-
        BlazeServerBuilder[IO]({
          val executor = Executors.newCachedThreadPool()
          ExecutionContext.fromExecutor(executor)
        }).bindHttp(conf.httpPort, "0.0.0.0")
          .withHttpApp((routes).orNotFound)
          .resource
          .use(_ => IO.never)
    } yield ()

  }
}
