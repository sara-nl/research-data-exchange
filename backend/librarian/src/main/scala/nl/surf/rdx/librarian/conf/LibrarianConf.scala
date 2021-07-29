package nl.surf.rdx.librarian.conf

import cats.effect.{Blocker, ContextShift, IO, Resource}
import pureconfig.ConfigSource
import pureconfig.module.catseffect2.syntax.CatsEffectConfigSource

import java.util.concurrent.Executors
import scala.concurrent.duration.FiniteDuration

object LibrarianConf {

  private val src = ConfigSource.default.at("librarian")

  // TODO: reuse
  private val blocker = Resource
    .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
    .map(Blocker.liftExecutorService)

  def loadIO(implicit cs: ContextShift[IO]): IO[LibrarianConf] = {
    import pureconfig.generic.auto._
    blocker.use(src.loadF[IO, LibrarianConf])
  }
}

case class LibrarianConf(
    httpPort: Int
)
