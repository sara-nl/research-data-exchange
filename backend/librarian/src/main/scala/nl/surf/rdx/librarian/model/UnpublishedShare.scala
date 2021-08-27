package nl.surf.rdx.librarian.model

import nl.surf.rdx.common.model.ShareToken

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object UnpublishedShare {

  object codecs {
    import io.circe._
    implicit val rdxShareEncoder: Encoder[UnpublishedShare] =
      Encoder.forProduct5("owner", "path", "createdAt", "conditionsDocument", "files")(share =>
        (
          share.owner,
          share.path,
          share.createdAt,
          share.conditionsDocument,
          share.files
        )
      )
  }

  def fromShare(shareToken: ShareToken, conditionsFileName: String): UnpublishedShare = {
    val (conditionsDocument, files) =
      separateConditions(shareToken.files, conditionsFileName)
    UnpublishedShare(
      shareToken.share.additional_info_owner,
      shareToken.share.path,
      shareToken.createdAt,
      conditionsDocument,
      files
    )
  }

  private[model] def separateConditions(
      files: List[String],
      fileName: String
  ): (Option[String], List[String]) =
    files.partition(_.toLowerCase.contains(fileName)) match {
      case (conditions, files) => (conditions.headOption, files)
    }
}

case class UnpublishedShare(
    owner: Option[String],
    path: String,
    createdAt: OffsetDateTime,
    conditionsDocument: Option[String],
    files: List[String]
)
