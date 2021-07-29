package nl.surf.rdx.common.model

import cats.effect.IO
import cats.implicits._
import nl.surf.rdx.common.model.owncloud.OwncloudShare

import java.time.LocalDateTime
import java.util.UUID

object ShareToken {
  def createFor(share: OwncloudShare): IO[ShareToken] =
    for {
      email <- IO.fromOption(share.additional_info_owner)(
        new RuntimeException(s"No additional info owner found for uid_owner ${share.uid_owner}")
      )
    } yield ShareToken(share, (LocalDateTime.now(), UUID.randomUUID()).some, email)
}

case class ShareToken(share: OwncloudShare, token: Option[(LocalDateTime, UUID)], email: String)
