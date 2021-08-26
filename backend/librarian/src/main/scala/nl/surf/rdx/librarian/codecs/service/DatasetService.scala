package nl.surf.rdx.librarian.codecs.service

import cats.data.Kleisli
import cats.effect.{Resource, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import cats.{Applicative, Functor}
import nl.surf.rdx.common.db.Shares
import nl.surf.rdx.common.model.{ShareToken, UserMetadata}
import nl.surf.rdx.librarian.codecs.service.DatasetService.Deps
import org.typelevel.log4cats.Logger
import skunk.Session
import skunk.data.Completion.Update

import java.util.UUID

object DatasetService {

  case class Deps[F[_]](session: Resource[F, Session[F]])

  def make[F[_]: Sync: Logger: Functor: Applicative]: Kleisli[F, Deps[F], DatasetService[F]] =
    Kleisli { deps: Deps[F] => Sync[F].pure(new DatasetService(deps)) }
}

class DatasetService[F[_]: Logger: Applicative: Sync: Functor](
    deps: Deps[F]
) {

  def publishShare(
      token: UUID,
      metadata: UserMetadata
  ): F[Unit] =
    (for {
      session <- deps.session
      data = (metadata, token)
      cmd <- session.prepare(Shares.update)
      res <- Resource.eval(cmd.execute(data))
      _ <- Resource.eval(
        res match {
          case Update(1) => Sync[F].unit
          case notUpdated =>
            Logger[F].warn(s"Failed to update share metadata. Query result is `$notUpdated`") >>
              Sync[F].raiseError(
                new RuntimeException("Failed to update share metadata")
              ) >>
              Sync[F].unit
        }
      )
    } yield ()).use(Sync[F].pure)

  def fetchShare(token: UUID): F[Option[ShareToken]] =
    (for {
      session <- deps.session
      cmd <- session.prepare(Shares.find(token))
      share <- Resource.eval(cmd.option(token))
    } yield share).use(Sync[F].pure(_))
}
