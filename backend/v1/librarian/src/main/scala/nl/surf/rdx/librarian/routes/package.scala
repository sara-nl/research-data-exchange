package nl.surf.rdx.librarian

import cats.implicits.catsSyntaxOptionId

package object routes {

  //  See "Handling path parameters" section here: https://http4s.org/v0.22/dsl/
  object extractors {

    // TODO: perform more validation here!
    object NonEmptyVar {
      def unapply(str: String): Option[String] = {
        if (str.nonEmpty)
          str.some
        else
          None
      }
    }
  }
}
