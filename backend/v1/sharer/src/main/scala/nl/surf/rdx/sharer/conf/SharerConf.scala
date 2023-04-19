package nl.surf.rdx.sharer.conf

import cats.effect.{Blocker, ContextShift, Sync}
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource

import scala.concurrent.duration.FiniteDuration

object SharerConf {

  def configSrc: CatsEffectConfigSource = ConfigSource.default.at("sharer")

  def loadF[F[_]: Sync: ContextShift]: F[SharerConf] = {
    Blocker[F].use(configSrc.loadF[F, SharerConf])
  }

}

case class SharerConf(
    webUrl: String,
    conditionsFileName: String,
    tokenValidityInterval: FiniteDuration,
    fetchInterval: FiniteDuration,
    tokenSweepInterval: FiniteDuration
)
