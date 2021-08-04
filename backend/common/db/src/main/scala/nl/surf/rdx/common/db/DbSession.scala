package nl.surf.rdx.common.db

import cats.effect.{Concurrent, ContextShift, IO, Resource, Sync}
import natchez.Trace
import nl.surf.rdx.common.db.conf.DbConf
import skunk.Session

object DbSession {

  def resource[F[_]: Sync: ContextShift: Concurrent: Trace]: Resource[F, Session[F]] =
    for {
      conf <- Resource.eval(DbConf.load[F])
      session <- Session.single(
        host = conf.host,
        port = conf.port,
        user = conf.user,
        database = conf.dbName,
        password = conf.password
      )
    } yield session
}
