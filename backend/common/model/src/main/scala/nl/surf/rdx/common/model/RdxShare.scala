package nl.surf.rdx.common.model

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import org.typelevel.log4cats.Logger

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

object RdxShare {
  def createFor[F[_]: Sync: Monad: Logger](
      share: OwncloudShare,
      files: List[String],
      validFor: FiniteDuration,
      conditionsUrl: String
  ): F[Option[RdxShare]] =
    (for {
      email <- Sync[F].fromOption(
        share.additional_info_owner,
        new RuntimeException(s"No additional info owner found for uid_owner ${share.uid_owner}")
      )
      now <- Sync[F].delay(OffsetDateTime.now())
    } yield RdxShare(
      share,
      now,
      UUID.randomUUID().some,
      now.plusHours(validFor.toHours),
      email,
      files,
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
