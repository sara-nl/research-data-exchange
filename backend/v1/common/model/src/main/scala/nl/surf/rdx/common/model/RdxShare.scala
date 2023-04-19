package nl.surf.rdx.common.model

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import org.typelevel.log4cats.Logger

import java.nio.file.{Path => JPath}
import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

object RdxShare {

  private def emailNotFoundError[F[_]: Sync, B](share: OwncloudShare) =
    Sync[F].raiseError[B](
      new RuntimeException(
        s"No email found for uid_owner ${share.uid_owner}"
      )
    )

  def createFor[F[_]: Sync: Monad: Logger](
      share: OwncloudShare,
      files: List[JPath],
      validFor: FiniteDuration,
      conditionsUrl: String
  ): F[Option[RdxShare]] =
    (for {
      email <-
        OptionT
          .fromOption[F](share.additional_info_owner)
          .getOrElseF(emailNotFoundError(share))
      email <-
        if (email.isEmpty)
          emailNotFoundError(share)
        else email.pure[F]
      now <- Sync[F].delay(OffsetDateTime.now())
    } yield RdxShare(
      share,
      now,
      UUID.randomUUID().some,
      now.plusHours(validFor.toHours),
      email,
      files.map(_.toString()),
      conditionsUrl
    )).redeemWith(
      Logger[F].warn(_)(
        s"Can not create email for share $share"
      ) >> none[RdxShare].pure[F],
      _.some.pure[F]
    )
}

case class RdxShare(
    share: OwncloudShare,
    createdAt: OffsetDateTime,
    token: Option[UUID],
    expiresAt: OffsetDateTime,
    email: String,
    files: List[String],
    conditionsUrl: String
)
