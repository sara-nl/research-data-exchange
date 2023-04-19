package nl.surf.rdx.common.testutils

import cats.effect.Sync
import org.typelevel.log4cats.slf4j.Slf4jLogger

object ConsoleLogger {
  // Impicit logger for most of the invocations of "code under test"
  implicit def loggerF[F[_]: Sync] = Slf4jLogger.getLogger[F]
}
