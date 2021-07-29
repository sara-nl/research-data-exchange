package nl.surf.rdx.sharer.email

import com.minosiants.pencil.data.{Body, Email, From, Mailbox, Subject, To}
import io.lemonlabs.uri.Uri

import java.time.{Duration, LocalDateTime, Period, ZonedDateTime}
import java.time.format.DateTimeFormatter

object RdxNotification {

  private val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm Z")

  def newShareHtml(
      recepientEmail: String,
      datasetName: String,
      publicationUrl: Uri,
      tokenExpiration: ZonedDateTime,
      rdxSupportEmail: String
  ): Email = {
    val tokenValidityHours = Duration.between(ZonedDateTime.now(), tokenExpiration).toHours
    val subject = Subject("New dataset available for publication")

    val html = s"""
      |<body style="font-size: 16px; font-family: Arial, sans-serif, 'Open Sans'">
      |<p>Dear $recepientEmail,</p>
      |
      |<p>A new dataset, "${datasetName}", has just been shared with RDX.</p>
      |
      |<p>You can now add the required metadata and publish it. That will make it available for download upon agreement to the associated use conditions.</p>
      |
      |<a style="font-size: 18px; color: #008cba;" href="$publicationUrl">Proceed to publication screen</a>
      |
      |
      |<p>Please note that the link above is valid for a limited amount of time and will expire in $tokenValidityHours hours, or on <span style="white-space: nowrap">
      |${tokenExpiration
      .format(formatter)}</span>. If you need more time, unshare the dataset, wait for a confirmation email and share it again.
      |</p>
      |<p>If you want to make changes to metadata after publication or need any other kind of support, please get in touch with the RDX support team: <a href="mailto:${rdxSupportEmail}">${rdxSupportEmail}</a>
      |</p>
      |</body>
      |""".stripMargin

    Email.mime(
      from = From(Mailbox.fromString(rdxSupportEmail).toOption.get),
      To(Mailbox.fromString(recepientEmail).toOption.get),
      subject,
      Body.Html(html)
    )
  }
}
sealed trait RdxNotification
