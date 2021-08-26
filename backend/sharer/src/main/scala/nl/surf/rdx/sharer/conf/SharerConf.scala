package nl.surf.rdx.sharer.conf

import cats.effect.{Blocker, ContextShift, IO, Resource, Sync}
import nl.surf.rdx.sharer.conf.SharerConf.{ClientConf, EmailConf}
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf
import pureconfig.ConfigSource
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

object SharerConf {

  def configSrc: CatsEffectConfigSource = ConfigSource.default.at("sharer")

  def loadF[F[_]: Sync: ContextShift]: F[SharerConf] = {
    Blocker[F].use(configSrc.loadF[F, SharerConf])
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
