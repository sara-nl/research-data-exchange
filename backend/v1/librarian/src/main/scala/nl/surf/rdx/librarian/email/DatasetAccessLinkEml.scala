package nl.surf.rdx.librarian.email

import cats.Monad
import cats.effect.Sync
import com.minosiants.pencil.data.{Attachment, Body, Mailbox, Subject, To}
import nl.surf.rdx.common.email.RdxEmail.Template
import nl.surf.rdx.common.email.RdxEmail.Template.{`</BODY>`, `<BODY>`}
import nl.surf.rdx.common.email.RdxEmail
import cats.implicits._
import nl.surf.rdx.common.model.access.RdxDownloadableDataset

import java.nio.file.{Path, Paths}

object DatasetAccessLinkEml {

  case class Vars[F[_]](
      requesterName: String,
      contactEmail: String,
      ds: RdxDownloadableDataset,
      mkLink: Path => F[String],
      downloadConditions: String => F[String]
  )

  def apply[F[_]: Sync: Monad]: Template[F, Vars] =
    RdxEmail.Template {
      case (conf, vars) =>
        for {
          conditionsLink <- vars.downloadConditions(vars.ds.conditionsUrl)
          attachment <- Attachment.fromString(conditionsLink)
          link <- vars.mkLink(Paths.get(vars.ds.owncloudShare.path))
          to <- Sync[F].fromOption(
            Mailbox.fromString(vars.contactEmail).toOption,
            new RuntimeException(s"Invalid email ${vars.contactEmail}")
          )
        } yield RdxEmail(
          To(to),
          Subject(s"Your access to ${vars.ds.owncloudShare.path}"),
          Body.Html(s"""
            |${`<BODY>`}
            |<p>Dear ${vars.requesterName},</p>
            |
            |<p>You can now download dataset ${vars.ds.owncloudShare.path}. Please use it responsibly and according to agreed conditions.</p>
            |
            |<a style="font-size: 18px; color: #008cba;" href="${link}">Download dataset</a>
            |
            |<p>You can find the conditions document in the attachment.</p>
            |
            |<p>Need help? Contact RDX support team: <a href="mailto:${conf.from}">${conf.from}</a>
            |
            |${`</BODY>`}
            |""".stripMargin),
          attachments = List(attachment)
        )
    }

}
