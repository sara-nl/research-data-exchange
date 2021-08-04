package nl.surf.rdx.common.model

import cats.effect.IO
import nl.surf.rdx.common.model.owncloud.OwncloudShare

import java.time.{LocalDateTime, OffsetDateTime}
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

object ShareToken {
  def createFor(
      share: OwncloudShare,
      files: List[String],
      validFor: FiniteDuration
  ): IO[ShareToken] =
    for {
      email <- IO.fromOption(share.additional_info_owner)(
        new RuntimeException(s"No additional info owner found for uid_owner ${share.uid_owner}")
      )
    } yield {
      val now = OffsetDateTime.now()
      val validTill = now.plusHours(validFor.toHours)
      ShareToken(share, now, UUID.randomUUID(), validTill, email, files)
    }
}

case class ShareToken(
    share: OwncloudShare,
    createdAt: OffsetDateTime,
    token: UUID,
    expiresAt: OffsetDateTime,
    email: String,
    files: List[String]
)
