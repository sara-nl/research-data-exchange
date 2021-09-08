package nl.surf.rdx.common.model.api

import cats.{FlatMap, Functor, Monad}
import cats.data.Kleisli
import cats.effect.Sync
import nl.surf.rdx.common.model.RdxShare

import java.time.OffsetDateTime
import cats.implicits._
object ShareInfo {
  case class Deps(conditionsFileName: String)

  private[api] def separateConditions(
      files: List[String],
      fileName: String
  ): (Option[String], List[String]) =
    files.partition(_.toLowerCase.contains(fileName)) match {
      case (conditions, files) => (conditions.headOption, files)
    }

  def fromShare[F[_]: Sync: Functor: Monad]: Kleisli[F, ShareInfo.Deps, RdxShare => F[ShareInfo]] =
    Kleisli {
      case Deps(conditionsFileName) =>
        Sync[F].pure((s: RdxShare) =>
          for {
            (conditionsOpt, files) <- Sync[F].pure(separateConditions(s.files, conditionsFileName))
            conditions <-
              Sync[F]
                .fromOption(
                  conditionsOpt,
                  new IllegalArgumentException(
                    s"Can not find conditions document in share ${s.share.id}"
                  )
                )
          } yield ShareInfo(s.share.path, s.email, s.createdAt, conditions, files)
        )
    }
}

/**
  * Share as seen via REST API.
  */
case class ShareInfo(
    path: String,
    owner: String,
    createdAt: OffsetDateTime,
    conditionsDocument: String,
    files: List[String]
)
