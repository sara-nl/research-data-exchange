package nl.surf.rdx.sharer.email

import cats.Monad
import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import com.minosiants.pencil.data.{Body, Mailbox, Subject, To}
import nl.surf.rdx.common.email.RdxEmail.Template._
import nl.surf.rdx.common.email.{RdxEmail}
import nl.surf.rdx.common.model.RdxShare

import java.time.{Duration, ZonedDateTime}

object NewDatasetEml {

  case class Vars[F[_]](webUrl: String, share: RdxShare)

  def apply[F[_]: Sync: Monad]: RdxEmail.Template[F, Vars] =
    Kleisli {
      case (conf, Vars(webUrl, share)) =>
        for {
          tokenValidityHours <-
            Sync[F].delay(Duration.between(ZonedDateTime.now(), share.expiresAt).toHours)
          to <- Sync[F].fromOption(
            Mailbox.fromString(share.email).toOption,
            new RuntimeException(s"Invalid email ${share.email}")
          )
          expiresAt = share.expiresAt.format(RdxEmail.Template.formatter)
        } yield RdxEmail(
          to = To(to),
          subject = Subject(s"Dataset ${share.share.path} available for publication"),
          body = Body.Html(s"""
                            |${`<BODY>`}
                            |<p>Dear ${share.email},</p>
                            |
                            |<p>A new dataset, "${share.share.path}", has just been shared with RDX.</p>
                            |
                            |<p>You can now add the required metadata and publish it. That will make it available for download upon agreement to the associated use conditions.</p>
                            |
                            |<a style="font-size: 18px; color: #008cba;" href="${webUrl}/publish/${share.token.get}">Proceed to publication screen</a>
                            |
                            |
                            |<p>Please note that the link above is valid for a limited amount of time and will expire in $tokenValidityHours hours, or on <span style="white-space: nowrap">
                            |$expiresAt</span>. If you need more time, unshare the dataset, wait for a confirmation email and share it again.
                            |</p>
                            |<p>If you want to make changes to metadata after publication or need any other kind of support, please get in touch with the RDX support team: <a href="mailto:${conf.from}">${conf.from}</a>
                            |</p>
                            |${`</BODY>`}
                            |""".stripMargin)
        )

    }
}
