package nl.surf.rdx.sharer

import cats.data.Kleisli
import cats.effect.{Blocker, IO, IOApp, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.{DexShares, OwncloudShares}
import org.http4s.BasicCredentials
import org.http4s.client.blaze.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import cats.implicits._
import nl.surf.rdx.common.db.{DbSession, MigrationApp, Shares}
import nl.surf.rdx.common.model.ShareToken
import nl.surf.rdx.sharer.email.{RdxEmail, RdxNotification}
import natchez.Trace.Implicits.noop
import nl.surf.rdx.sharer.owncloud.DexShares.Observation

object SharerApp extends IOApp.Simple {

  type EnvF[F[_], C] = Kleisli[F, Deps, C]

  case class Deps(sharesDeps: OwncloudShares.Deps, conf: SharerConf)

  private implicit val logger = Slf4jLogger.getLogger[IO]
  private implicit val loggerP = Slf4jLogger.getLogger[EnvF[IO, *]]

  val run: IO[Unit] = {

    // After start, it should send email for shares that haven't received a notification yet
    // It should detect new shares and send emails for each of them...

// TODO: Retry for shares fetch
    logger.info("Sharer service starting") >> (for {
      _ <- MigrationApp.run
      conf <- SharerConf.loadIO
      ocDeps = OwncloudShares.Deps(
        BlazeClientBuilder[IO](ExecutionContext.global)
          .withConnectTimeout(conf.client.connectionTimeout)
          .withResponseHeaderTimeout(conf.client.responseHeaderTimeout)
          .withRequestTimeout(conf.client.requestTimeout)
          .withIdleTimeout(conf.client.idleTimeout)
          .resource,
        BasicCredentials(conf.owncloud.webdavUsername, conf.owncloud.webdavPassword),
        conf.owncloud
      )
      deps = Deps(ocDeps, conf)
      _ <-
        DexShares.stream
          .run(deps)
          .evalTap(observedDexShares =>
            DbSession
              .resource[EnvF[IO, *]]
              .use(session => {
                for {
                  storedShares <- session.execute(Shares.list)
                  _ <- loggerP.info(
                    s"Observed ${observedDexShares.size} shares, found ${storedShares.size} stored shares"
                  )
                  compared = Merge(storedShares.toSet, observedDexShares.toSet)
                  newShareTokens <-
                    compared.added.toList
                      .parTraverse {
                        case Observation(share, files) =>
                          Kleisli.liftF[IO, Deps, Option[ShareToken]](
                            ShareToken
                              .createFor(share, files, conf.tokenValidityInterval)
                              .redeemWith(
                                logger.warn(_)(
                                  "Skipping user's share because email is not available"
                                ) >> None.pure[IO],
                                Some(_).pure[IO]
                              )
                          )
                      }
                      .map(_.flatten)

                  sharesToRemove = compared.removed.toList
                  _ <-
                    if (sharesToRemove.nonEmpty)
                      session
                        .prepare(Shares.delete(sharesToRemove))
                        .use(_.execute(sharesToRemove))
                    else
                      Sync[EnvF[IO, *]].unit
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
                          .flatTap(_ => loggerP.debug("Email sent"))
                      )
                      .parSequence

                } yield ()
              })
          )
          .compile
          .drain
          .run(deps)
    } yield ())

  }
}
