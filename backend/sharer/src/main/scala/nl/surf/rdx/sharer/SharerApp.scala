package nl.surf.rdx.sharer

import cats.data.Kleisli
import cats.effect.concurrent.Ref
import cats.effect.{Blocker, IO, IOApp}
import cats.implicits.catsSyntaxFlatMapOps
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.{DexShares, OwncloudShares}
import nl.surf.rdx.sharer.owncloud.webdav.Webdav
import nl.surf.rdx.sharer.owncloud.webdav.Webdav.implicits._
import org.http4s.BasicCredentials
import org.http4s.client.blaze.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.lemonlabs.uri.typesafe.dsl._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import cats.implicits._
import com.minosiants.pencil.Client
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
import nl.surf.rdx.common.model.ShareToken
import nl.surf.rdx.sharer.email.{RdxEmail, RdxNotification}

import java.time.{LocalDateTime, ZonedDateTime}

object SharerApp extends IOApp.Simple {

  private implicit val logger = Slf4jLogger.getLogger[IO]

  val run: IO[Unit] = {

    // After start, it should send email for shares that haven't received a notification yet
    // It should detect new shares and send emails for each of them...

// TODO: Retry for shares fetch
    logger.info("Sharer service starting") >> (for {
      conf <- SharerConf.loadIO
      shareTokens <- Ref[IO].of((false, Set.empty[ShareToken]))
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
      _ <-
        fs2.Stream
          .awakeEvery[IO](conf.fetchInterval)
          .evalTap(_ => logger.debug(s"Starting shares fetch"))
          .evalTap(_ =>
            for {
              observedDexShares <- DexShares.observe.run(DexShares.Deps(ocDeps, conf))
              ((initialized, storedShares), updateState) <- shareTokens.access
              _ <- logger.info(
                s"Observed ${observedDexShares.size} shares, found ${storedShares.size} stored shares"
              )
              compared = Merge(storedShares.map(_.share), observedDexShares.toSet)
              added <-
                compared.added
                  .map(ShareToken.createFor)
                  .map(
                    _.redeemWith(
                      logger.warn(_)(
                        "Skipping user's share because email is not available"
                      ) >> None.pure[IO],
                      Some(_).pure[IO]
                    )
                  )
                  .toList
                  .sequence
                  .map(_.flatten)
              _ <- updateState(
                (
                  true,
                  storedShares.filterNot(t => compared.removed.contains(t.share)) ++ added.toSet
                )
              )
              _ <- logger.info(
                s"Update finished. +${compared.added.size} shares / -${compared.removed.size} shares"
              )
              _ <-
                if (initialized) {
                  added
                    .map { t =>
                      val html = RdxNotification.newShareHtml(
                        t.email,
                        t.share.path,
                        "https://google.com",
                        ZonedDateTime.now().plusHours(conf.tokenValidityInterval.toHours),
                        conf.email.from
                      )
                      RdxEmail
                        .send(html)
                        .flatTap(_ =>
                          Kleisli.liftF[IO, SharerConf.EmailConf, Unit](logger.debug("Email sent."))
                        )
                    }
                    .parSequence
                    .run(conf.email)
                } else {
                  logger.info("Don't send any emails on initialization")
                }

            } yield ()
          )
          .compile
          .drain
    } yield ())

  }
}
