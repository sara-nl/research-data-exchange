package nl.surf.rdx.sharer.email

import cats.data.Kleisli
import cats.effect.Sync
import com.minosiants.pencil.data.{Body, Email, From, Mailbox, Subject, To}
import io.lemonlabs.uri.Uri
import nl.surf.rdx.common.model.RdxShare
import nl.surf.rdx.sharer.conf.SharerConf

import java.time.{Duration, LocalDateTime, Period, ZonedDateTime}
import java.time.format.DateTimeFormatter

object RdxNotification {

  private val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm Z")

  def newToken[F[_]: Sync](
      token: RdxShare
  ): Kleisli[F, SharerConf, Email] =
    Kleisli { conf =>
      val tokenValidityHours = Duration.between(ZonedDateTime.now(), token.expiresAt).toHours
      val subject = Subject("New dataset available for publication")

      val html = s"""
      |<body style="font-size: 16px; font-family: Arial, sans-serif, 'Open Sans'">
      |<p>Dear ${token.email},</p>
      |
      |<p>A new dataset, "${token.share.path}", has just been shared with RDX.</p>
      |
      |<p>You can now add the required metadata and publish it. That will make it available for download upon agreement to the associated use conditions.</p>
      |
      |<a style="font-size: 18px; color: #008cba;" href="${conf.webUrl}/publish/${token.token.get}">Proceed to publication screen</a>
      |
      |
      |<p>Please note that the link above is valid for a limited amount of time and will expire in $tokenValidityHours hours, or on <span style="white-space: nowrap">
      |${token.expiresAt
        .format(formatter)}</span>. If you need more time, unshare the dataset, wait for a confirmation email and share it again.
      |</p>
      |<p>If you want to make changes to metadata after publication or need any other kind of support, please get in touch with the RDX support team: <a href="mailto:${conf.email.from}">${conf.email.from}</a>
      |</p>
      |</body>
      |""".stripMargin

      Sync[F].delay {
        Email.mime(
          from = From(Mailbox.fromString(conf.email.from).toOption.get),
          To(Mailbox.fromString(token.email).toOption.get),
          subject,
          Body.Html(html)
        )
      }

    }
}
