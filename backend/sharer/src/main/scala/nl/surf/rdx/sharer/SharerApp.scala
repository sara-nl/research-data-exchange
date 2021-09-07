package nl.surf.rdx.sharer

import cats.{Applicative, Parallel}
import cats.data.Kleisli
import cats.effect.{Blocker, Concurrent, ContextShift, IO, IOApp, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.{OwncloudShares, OwncloudSharesObserver}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.language.postfixOps
import cats.implicits._
import natchez.Trace
import nl.surf.rdx.common.db.{DbSession, MigrationApp, Shares}
import nl.surf.rdx.common.model.RdxShare
import nl.surf.rdx.sharer.email.{RdxEmail, RdxNotification}
import natchez.Trace.Implicits.noop
import nl.surf.rdx.core.RdxScheduler
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.{Deps, Observation}
import nl.surf.rdx.sharer.owncloud.webdav.Webdav
import org.typelevel.log4cats.Logger

object SharerApp extends IOApp.Simple {

  type EnvF[F[_], C] = Kleisli[F, Deps, C]

  case class Deps(conf: SharerConf)

  private implicit def loggerF[F[_]: Sync] = Slf4jLogger.getLogger[F]

  val run: IO[Unit] =
    for {
      _ <-
        Logger[IO]
          .info("Sharer service starting")
      _ <- Logger[IO].debug("Debug logging enabled")
      _ <- MigrationApp.run
      conf <- SharerConf.loadF[IO]
      fiber1 <- RdxScheduler.start[IO]("share token sweeper", conf.tokenSweepInterval)(
        DbSession.resource[IO].use(session => session.execute(Shares.invalidateAllExpired))
      )
      fiber2 <-
        RdxScheduler
          .stream[EnvF[IO, *], List[Observation]]("share observer", conf.fetchInterval)(
            OwncloudSharesObserver.observe.local { deps: Deps =>
              OwncloudSharesObserver.Deps(
                Webdav.makeSardine[IO].run(conf.owncloud),
                OwncloudShares.getShares[IO].run(deps),
                conf.owncloud.webdavBase,
                conf.conditionsFileName
              )
            }
          )
          .through(SharePipes.onlyElegible[IO])
          .evalTap(SharePipes.doMergeShares[IO])
          .compile
          .drain
          .run(Deps(conf))
          .start
      _ <- List(fiber1, fiber2).map(_.join).parSequence
    } yield ()

}
