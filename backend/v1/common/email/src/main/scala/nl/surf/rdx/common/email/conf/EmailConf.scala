package nl.surf.rdx.common.email.conf

import cats.effect.{Blocker, ContextShift, Sync}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource

object EmailConf {
  private val src = ConfigSource.default.at("email")

  def loadF[F[_]: Sync: ContextShift]: F[EmailConf] = {
    import pureconfig.generic.auto._
    Blocker[F].use(src.loadF[F, EmailConf])
  }
}

case class EmailConf(
    host: String,
    port: Int,
    user: Option[String],
    password: Option[String],
    from: String
)
