package nl.surf.rdx.common.owncloud.conf

import cats.effect.{Blocker, ContextShift, Sync}
import nl.surf.rdx.common.owncloud.conf.OwncloudConf.{ClientConf, WebdavBase}
import pureconfig.ConfigSource
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

case class OwncloudConf(
    webdavUsername: String,
    webdavPassword: String,
    sharesSource: String,
    maxFolderDepth: Short,
    webdavBase: WebdavBase,
    minimumPermissionLevel: Byte,
    client: ClientConf
)

object OwncloudConf {

  case class ClientConf(
      idleTimeout: FiniteDuration,
      requestTimeout: FiniteDuration,
      connectionTimeout: FiniteDuration,
      responseHeaderTimeout: FiniteDuration
  )

  def configSrc: CatsEffectConfigSource = ConfigSource.default.at("owncloud")

  def loadF[F[_]: Sync: ContextShift]: F[OwncloudConf] = {
    Blocker[F].use(configSrc.loadF[F, OwncloudConf])
  }

  case class WebdavBase(serverUri: String, serverSuffix: String)

}
