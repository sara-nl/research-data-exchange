package nl.surf.rdx.common.model

import cats.{FlatMap, Functor, Monad}
import cats.effect.{IO, Sync}
import nl.surf.rdx.common.model.owncloud.OwncloudShare

import java.time.{LocalDateTime, OffsetDateTime}
import java.util.UUID
import scala.concurrent.duration.FiniteDuration
import cats.implicits._
import org.typelevel.log4cats.Logger

object ShareToken {

  def createFor[F[_]: Sync: Monad: Logger](
      share: OwncloudShare,
      files: List[String],
      validFor: FiniteDuration
  ): F[Option[ShareToken]] =
    (for {
      email <- Sync[F].fromOption(
        share.additional_info_owner,
        new RuntimeException(s"No additional info owner found for uid_owner ${share.uid_owner}")
      )
      now <- Sync[F].delay(OffsetDateTime.now())
    } yield ShareToken(
      share,
      now,
      UUID.randomUUID().some,
      now.plusHours(validFor.toHours),
      email,
      files
    )).redeemWith(
      Logger[F].warn(_)(
        s"Can not create email for share $share"
      ) >> none[ShareToken].pure[F],
      _.some.pure[F]
    )
}

case class ShareToken(
    share: OwncloudShare,
    createdAt: OffsetDateTime,
    token: Option[UUID],
    expiresAt: OffsetDateTime,
    email: String,
    files: List[String]
)