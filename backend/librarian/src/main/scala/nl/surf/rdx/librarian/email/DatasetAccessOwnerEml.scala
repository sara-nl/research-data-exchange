package nl.surf.rdx.librarian.email

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import com.minosiants.pencil.data._
import nl.surf.rdx.common.email.RdxEmail
import nl.surf.rdx.common.email.RdxEmail.Template
import nl.surf.rdx.common.email.RdxEmail.Template.{`</BODY>`, `<BODY>`}
import nl.surf.rdx.common.model.access.RdxDownloadableDataset

object DatasetAccessOwnerEml {

  case class Vars[F[_]](
      requester: String,
      requesterEmail: String,
      ds: RdxDownloadableDataset
  )

  def apply[F[_]: Sync: Monad]: Template[F, Vars] =
    RdxEmail.Template {
      case (conf, vars) =>
        for {
          email <- vars.ds.owncloudShare.additional_info_owner.pure[F]
          to <- Sync[F].fromOption(
            Mailbox.fromString(email.orEmpty).toOption,
            new RuntimeException(s"Invalid email ${email.orEmpty}")
          )
        } yield RdxEmail(
          To(to),
          Subject(s"Dataset '${vars.ds.owncloudShare.path}' was requested"),
          Body.Html(s"""
            |${`<BODY>`}
            |<p>Dear User,</p>
            |
            |<p>For your information, we received an access request for your dataset '${vars.ds.owncloudShare.path}' from ${vars.requester} &lt;${vars.requesterEmail}&gt;.</p>
            |<p> This is an informational email, no action is required.
            |<p>Need help? Contact RDX support team: <a href="mailto:${conf.from}">${conf.from}</a>
            |
            |${`</BODY>`}
            |""".stripMargin),
          attachments = List()
        )
    }

}
