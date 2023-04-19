package nl.surf.rdx.librarian.error

import org.http4s.Status

/**
  * This is an exception that can be used for quickly capturing the fact of
  * an error while handling a request (e.g. validation, runtime, etc) along with its
  * stacktrace and the desired response code and message.
  * IMPORTANT: don't expose and sensitive information in the message as it will be sent via HTTP.
  */
final case class PublicRouteError[F[_]](
    status: Status,
    private val cause: Throwable = None.orNull
) extends Exception(status.reason, cause)
