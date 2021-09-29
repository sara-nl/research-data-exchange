package nl.surf.rdx.librarian.error

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}
import org.typelevel.log4cats.Logger
import skunk.exception.SkunkException

object RoutesErrorHandler {

  def apply[F[_]: Logger: MonadError[*[_], Throwable]] = new RoutesErrorHandler[F]

}

class RoutesErrorHandler[F[_]: Logger: MonadError[*[_], Throwable]] extends Http4sDsl[F] {

  private val E = MonadError[F, Throwable]

  private val handler: Throwable => F[Response[F]] = {
    case PublicRouteError(status, cause) =>
      // Convert PRE-errors to HTTP responses
      Logger[F].warn(cause)("Foreseen error while handing the route") >>
        Response[F](status).pure[F]
    case error: SkunkException =>
      // Show nice Skunk stacktrace
      Logger[F].info(error.toString) >> E.raiseError[Response[F]](error)
    case cause =>
      E.raiseError[Response[F]](cause)

  }

  def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli(req =>
      OptionT(routes.run(req).value.handleErrorWith(e => handler(e).map(Option.apply)))
    )

}
