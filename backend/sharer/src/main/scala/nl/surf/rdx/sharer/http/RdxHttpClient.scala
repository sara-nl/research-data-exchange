package nl.surf.rdx.sharer.http

import cats.data.Kleisli
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Sync}
import io.circe.Json
import natchez.Trace
import nl.surf.rdx.common.db.conf.DbConf
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.conf.SharerConf.ClientConf
import org.http4s.{EntityDecoder, Request}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object RdxHttpClient {

  def resourceK[F[_]: ContextShift: ConcurrentEffect]
      : Kleisli[Resource[F, *], ClientConf, Client[F]] =
    Kleisli(conf =>
      BlazeClientBuilder[F](ExecutionContext.global)
        .withConnectTimeout(conf.connectionTimeout)
        .withResponseHeaderTimeout(conf.responseHeaderTimeout)
        .withRequestTimeout(conf.requestTimeout)
        .withIdleTimeout(conf.idleTimeout)
        .resource
    )

  def runRequest[F[_]: ContextShift: ConcurrentEffect](
      request: Request[F]
  ) = {
    import org.http4s.circe.CirceEntityDecoder._
    RdxHttpClient
      .resourceK[F]
      .mapF(
        _.use(client => client.expect[Json](request))
      )
  }

}
