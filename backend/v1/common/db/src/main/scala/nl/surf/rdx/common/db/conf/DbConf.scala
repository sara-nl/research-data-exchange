package nl.surf.rdx.common.db.conf

import cats.effect.{ContextShift, IO, Sync}
import nl.surf.rdx.common.db.conf.DbConf.FlywayConf
import pureconfig.ConfigSource
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource
import pureconfig.generic.auto._

object DbConf {
  final case class FlywayConf(
      url: String,
      driver: String,
      migrationsTable: String,
      migrationsLocations: List[String]
  )

  def load[F[_]: Sync: ContextShift]: F[DbConf] =
    Sync[F].delay {
      ConfigSource.default.at("common.db").loadOrThrow[DbConf]
    }
}

case class DbConf(
    host: String,
    port: Int,
    dbName: String,
    user: String,
    password: Option[String],
    flyway: FlywayConf
)
