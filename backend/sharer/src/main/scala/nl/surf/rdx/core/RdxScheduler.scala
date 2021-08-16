package nl.surf.rdx.core

import cats.effect.{Concurrent, ContextShift, Fiber, Sync, Timer}
import cats.implicits._
import cats.effect.implicits._
import natchez.Trace
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration

object RdxScheduler {

  def start[F[_]: Timer: Logger: ContextShift: Trace: Concurrent](
      label: String,
      interval: FiniteDuration
  )(thunk: => F[_]): F[Fiber[F, Unit]] =
    stream[F, Unit](label, interval)(thunk >> Sync[F].unit).compile.drain.start

  def stream[F[_]: Timer: Logger: ContextShift: Trace: Concurrent, O](
      label: String,
      interval: FiniteDuration
  )(thunk: => F[O]): fs2.Stream[F, O] =
    fs2.Stream.eval(Logger[F].info(s"⏰ Starting $label with interval $interval")) >>
      fs2.Stream
        .awakeEvery[F](interval)
        .evalTap(_ => Logger[F].debug(s"Started $label round"))
        .evalMap(_ => thunk)
        .evalTap(c => Logger[F].debug(s"Completed $label round with $c"))

}
