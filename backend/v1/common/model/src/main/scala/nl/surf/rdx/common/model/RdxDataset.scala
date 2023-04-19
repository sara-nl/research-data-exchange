package nl.surf.rdx.common.model

object RdxDataset {

  object codecs {
    import io.circe._
    implicit val rdxDsEncoder: Encoder[RdxDataset] =
      Encoder.forProduct4("title", "description", "conditionsUrl", "files")(ds =>
        (
          ds.title,
          ds.description,
          ds.conditionsUrl,
          ds.files
        )
      )

  }
}

/**
  * Dataset published in RDX
  */
case class RdxDataset(
    title: String,
    description: String,
    conditionsUrl: String,
    files: Seq[String]
)
