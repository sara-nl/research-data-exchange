package nl.surf.rdx.common.model

object UserMetadata {
  object codecs {
    import io.circe._
    implicit val userMetaData: Encoder[UserMetadata] =
      Encoder.forProduct4("doi", "title", "authors", "description")(ds =>
        (
          ds.doi,
          ds.title,
          ds.authors,
          ds.description
        )
      )

    implicit val userMetadataDecoder: Decoder[UserMetadata] =
      Decoder.forProduct4("doi", "title", "authors", "description")(
        (
            doi: String,
            title: String,
            authors: String,
            description: String
        ) => UserMetadata(doi, title, authors, description)
      )

  }
}

case class UserMetadata(doi: String, title: String, authors: String, description: String)
