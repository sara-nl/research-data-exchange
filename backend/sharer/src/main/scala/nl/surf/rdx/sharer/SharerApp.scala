package nl.surf.rdx.sharer

import cats.{Applicative, Parallel}
import cats.data.Kleisli
import cats.effect.{Blocker, Concurrent, ContextShift, IO, IOApp, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.language.postfixOps
import cats.implicits._
import natchez.Trace
import nl.surf.rdx.common.db.{DbSession, MigrationApp, Shares}
import nl.surf.rdx.common.model.RdxShare
import nl.surf.rdx.sharer.email.NewDatasetEml
import natchez.Trace.Implicits.noop
import nl.surf.rdx.common.email.conf.EmailConf
import nl.surf.rdx.common.owncloud.OwncloudShares
import nl.surf.rdx.common.owncloud.conf.OwncloudConf
import nl.surf.rdx.core.RdxScheduler
import nl.surf.rdx.sharer.owncloud.webdav.Webdav
import org.typelevel.log4cats.Logger

object SharerApp extends IOApp.Simple {

  type EnvF[F[_], C] = Kleisli[F, Deps, C]

  case class Deps(conf: SharerConf, emailConf: EmailConf, ocConf: OwncloudConf)

  private implicit def loggerF[F[_]: Sync] = Slf4jLogger.getLogger[F]

  val run: IO[Unit] =
    for {
      _ <-
        Logger[IO]
          .info("Sharer service starting")
      _ <- Logger[IO].debug("Debug logging enabled")
      _ <- MigrationApp.run
      (conf, emailConf, ocConf) <-
        (SharerConf.loadF[IO], EmailConf.loadF[IO], OwncloudConf.loadF[IO]).parTupled
      sweeperFiber <- RdxScheduler.start[IO]("share token sweeper", conf.tokenSweepInterval)(
        DbSession.resource[IO].use(session => session.execute(Shares.invalidateAllExpired))
      )
      observerFiber <-
        RdxScheduler
          .stream("share observer", conf.fetchInterval)(
            OwncloudSharesObserver
              .observe[IO]
              .local[Deps](_ =>
                OwncloudSharesObserver.Deps(
                  Webdav.makeSardine[IO].run(ocConf),
                  OwncloudShares.getShares[IO].run(ocConf),
                  ocConf.webdavBase,
                  conf.conditionsFileName
                )
              )
          )
          .through(SharePipes.onlyElegible[IO])
          .evalTap(SharePipes.doMergeShares[IO])
          .compile
          .drain
          .run(Deps(conf, emailConf, ocConf))
          .start
      _ <- List(sweeperFiber, observerFiber).map(_.join).parSequence
    } yield ()

}
