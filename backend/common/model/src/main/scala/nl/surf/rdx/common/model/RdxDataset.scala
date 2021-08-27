package nl.surf.rdx.common.model

import io.lemonlabs.uri.{AbsoluteUrl, RelativeUrl}

object RdxDataset {

  object codecs {
    import io.circe._
    implicit val rdxDsEncoder: Encoder[RdxDataset] =
      Encoder.forProduct5("doi", "title", "description", "conditionsDocument", "files")(ds =>
        (
          ds.doi.toStringPunycode,
          ds.title,
          ds.description,
          ds.conditionsDocument.toStringPunycode,
          ds.files
        )
      )

    implicit val rdxDsDecoder: Decoder[RdxDataset] =
      Decoder.forProduct5("doi", "title", "description", "conditionsDocument", "files")(
        (
            doi: String,
            title: String,
            description: String,
            conditionsDocument: String,
            files: Seq[String]
        ) =>
          (for {
            doiUrl <- RelativeUrl.parseTry(doi)
            conditionsDocumentUrl <- AbsoluteUrl.parseTry(conditionsDocument)
            // TODO: fix .get
          } yield RdxDataset(doiUrl, title, description, conditionsDocumentUrl, files)).get
      )
  }
}

case class RdxDataset(
    doi: RelativeUrl,
    title: String,
    description: String,
    conditionsDocument: AbsoluteUrl,
    files: Seq[String]
)
