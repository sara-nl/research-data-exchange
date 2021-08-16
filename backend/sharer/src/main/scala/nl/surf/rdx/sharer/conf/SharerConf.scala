package nl.surf.rdx.sharer.conf

import cats.effect.{Blocker, ContextShift, IO, Resource}
import nl.surf.rdx.sharer.conf.SharerConf.{ClientConf, EmailConf}
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf
import pureconfig.ConfigSource
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource
import pureconfig.generic.auto._

import java.util.concurrent.Executors
import scala.concurrent.duration.FiniteDuration

object SharerConf {

  protected val blocker = Resource
    .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
    .map(Blocker.liftExecutorService)

  def configSrc: CatsEffectConfigSource = ConfigSource.default.at("sharer")

  def loadIO(implicit cs: ContextShift[IO]): IO[SharerConf] = {
    Blocker[IO].use(configSrc.loadF[IO, SharerConf])
  }

  case class ClientConf(
      idleTimeout: FiniteDuration,
      requestTimeout: FiniteDuration,
      connectionTimeout: FiniteDuration,
      responseHeaderTimeout: FiniteDuration
  )

  case class EmailConf(
      host: String,
      port: Int,
      user: Option[String],
      password: Option[String],
      from: String
  )

}

case class SharerConf(
    webUrl: String,
    conditionsFileName: String,
    tokenValidityInterval: FiniteDuration,
    owncloud: OwncloudConf,
    client: ClientConf,
    fetchInterval: FiniteDuration,
    tokenSweepInterval: FiniteDuration,
    email: EmailConf
)
