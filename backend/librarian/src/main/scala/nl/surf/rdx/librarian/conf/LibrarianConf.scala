package nl.surf.rdx.librarian.conf

import cats.effect.{Blocker, ContextShift, IO, Resource, Sync}
import pureconfig.ConfigSource
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource

object LibrarianConf {

  private val src = ConfigSource.default.at("librarian")

  def loadF[F[_]: Sync: ContextShift]: F[LibrarianConf] = {
    import pureconfig.generic.auto._
    Blocker[F].use(src.loadF[F, LibrarianConf])
  }
}

case class LibrarianConf(
    httpPort: Int,
    conditionsFileName: String
)
