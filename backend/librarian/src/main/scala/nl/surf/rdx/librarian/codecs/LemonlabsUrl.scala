package nl.surf.rdx.librarian.codecs

import io.circe.{Encoder, Json}
import io.lemonlabs.uri.AbsoluteUrl

trait LemonlabsUrl {
  implicit val lemonlabsUrlEncoder =
    Encoder[AbsoluteUrl](url => Json.fromString(url.toStringPunycode))
}
