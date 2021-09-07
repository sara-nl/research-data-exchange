package nl.surf.rdx.common.model.api

import cats.Functor
import cats.data.Kleisli
import cats.effect.Sync
import nl.surf.rdx.common.model.RdxShare

import java.time.OffsetDateTime
import cats.implicits._
object ShareInfo {
  case class Deps(conditionsFileName: String)

  private def separateConditions(
      files: List[String]
  )(implicit fileName: String): (Option[String], List[String]) =
    files.partition(_.toLowerCase.contains(fileName)) match {
      case (conditions, files) => (conditions.headOption, files)
    }

  def fromShare[F[_]: Sync: Functor]: Kleisli[F, ShareInfo.Deps, RdxShare => F[ShareInfo]] =
    Kleisli {
      case Deps(conditionsFileName) =>
        Sync[F].pure((s: RdxShare) =>
          Sync[F]
            .fromOption(
              separateConditions(s.files)(conditionsFileName)._1,
              new RuntimeException(s"Can not find conditions document in share ${s.share.id}")
            )
            .map(ShareInfo(s.share.path, s.email, s.createdAt, _, s.files))
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
