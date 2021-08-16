package nl.surf.rdx.sharer

import cats.data.Kleisli
import cats.effect.{Blocker, IO, IOApp, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.{OwncloudShares, OwncloudSharesObserver}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.language.postfixOps
import cats.implicits._
import nl.surf.rdx.common.db.{DbSession, MigrationApp, Shares}
import nl.surf.rdx.common.model.ShareToken
import nl.surf.rdx.sharer.email.{RdxEmail, RdxNotification}
import natchez.Trace.Implicits.noop
import nl.surf.rdx.core.RdxScheduler
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.{Observation, Deps}
import nl.surf.rdx.sharer.owncloud.webdav.Webdav
import org.typelevel.log4cats.Logger

object SharerApp extends IOApp.Simple {

  type EnvF[F[_], C] = Kleisli[F, Deps, C]
  type KIO[C] = EnvF[IO, C]

  case class Deps(conf: SharerConf)

  private implicit val logger = Slf4jLogger.getLogger[IO]
  private implicit val loggerP = Slf4jLogger.getLogger[KIO]

  val run: IO[Unit] =
    logger.info("Sharer service starting") >> logger.debug("Debug logging enabled") >> (for {
      _ <- MigrationApp.run
      conf <- SharerConf.loadIO
      fiber1 <- RdxScheduler.start[IO]("share token sweeper", conf.tokenSweepInterval)(
        DbSession.resource[IO].use(session => session.execute(Shares.invalidateAllExpired))
      )
      fiber2 <-
        RdxScheduler
          .stream[KIO, List[Observation]]("share observer", conf.fetchInterval)(
            OwncloudSharesObserver.observe.local { deps: Deps =>
              OwncloudSharesObserver.Deps(
                Webdav.makeSardine[IO].run(conf.owncloud),
                OwncloudShares.getShares[IO].run(deps),
                conf.owncloud.webdavBase,
                conf.conditionsFileName
              )
            }
          )
          .evalTap(doObserveShares)
          .compile
          .drain
          .run(Deps(conf))
          .start
      _ <- List(fiber1, fiber2).map(_.join).parSequence
    } yield ())

  private def doObserveShares(observedDexShares: List[Observation]): KIO[Unit] =
    Kleisli.ask[IO, Deps].flatMap {
      case Deps(conf) =>
        DbSession
          .resource[KIO]
          .use(session => {
            for {
              storedShares <- session.execute(Shares.list)
              _ <- Logger[KIO].info(
                s"Observed ${observedDexShares.size} shares, found ${storedShares.size} stored shares"
              )
              compared = Merge(storedShares.toSet, observedDexShares.toSet)
              newShareTokens <-
                compared.added.toList
                  .parTraverseFilter {
                    case Observation(share, files) =>
                      ShareToken.createFor[KIO](share, files, conf.tokenValidityInterval)
                  }
              sharesToRemove = compared.removed.toList
              _ <-
                if (sharesToRemove.nonEmpty)
                  session
                    .prepare(Shares.delete(sharesToRemove))
                    .use(_.execute(sharesToRemove))
                else
                  Sync[KIO].unit
              _ <-
                session
                  .prepare(Shares.add)
                  .use(prep => newShareTokens.map(prep.execute).sequence)
              _ <- loggerP.info(
                s"Update finished. +${compared.added.size} shares / -${compared.removed.size} shares"
              )
              emailMessages <-
                newShareTokens
                  .map(RdxNotification.newToken[IO])
                  .parSequence
                  .local[Deps](_.conf)
              _ <-
                emailMessages
                  .map(
                    RdxEmail
                      .send(_)
                      .local[Deps](_.conf.email)
                      .flatTap(_ => loggerP.debug("Emails sent"))
                  )
                  .parSequence

            } yield ()
          })
    }
}
