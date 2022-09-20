package nl.surf.rdx.common.owncloud.http

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Sync}
import io.circe.Json
import nl.surf.rdx.common.owncloud.conf.OwncloudConf.ClientConf
import org.http4s.Request
import org.http4s.blaze.client._
import scala.concurrent.ExecutionContext

object RdxHttpClient {

  private def builderK[F[_]: ContextShift: ConcurrentEffect]
      : Kleisli[F, ClientConf, BlazeClientBuilder[F]] =
    Kleisli.fromFunction(conf =>
      BlazeClientBuilder[F](ExecutionContext.global)
        .withConnectTimeout(conf.connectionTimeout)
        .withResponseHeaderTimeout(conf.responseHeaderTimeout)
        .withRequestTimeout(conf.requestTimeout)
        .withIdleTimeout(conf.idleTimeout)
    )

  def runRequest[F[_]: ContextShift: ConcurrentEffect]
      : Kleisli[F, ClientConf, Request[F] => F[Json]] = {
    import org.http4s.circe.CirceEntityDecoder._
    for {
      builder <- RdxHttpClient.builderK[F]
    } yield (r: Request[F]) => builder.resource.use(_.expect[Json](r))
  }

}
