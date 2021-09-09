package nl.surf.rdx.common.db

import cats.data.Kleisli
import cats.effect.{Concurrent, ContextShift, Resource}
import natchez.Trace
import nl.surf.rdx.common.db.conf.DbConf
import skunk.Session

object DbSession {

  def resourceK[F[_]: ContextShift: Concurrent: Trace]
      : Kleisli[Resource[F, *], DbConf, Session[F]] =
    Kleisli(conf =>
      Session.single(
        host = conf.host,
        port = conf.port,
        user = conf.user,
        database = conf.dbName,
        password = conf.password
      )
    )

  def resource[F[_]: ContextShift: Concurrent: Trace]: Resource[F, Session[F]] =
    for {
      conf <- Resource.eval(DbConf.load[F])
      res <- resourceK.run(conf)
    } yield res
}
