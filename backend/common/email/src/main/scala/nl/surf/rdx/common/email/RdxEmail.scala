package nl.surf.rdx.common.email

import cats.data.Kleisli
import cats.effect.{Blocker, Concurrent, ContextShift, Sync}
import com.minosiants.pencil.Client
import com.minosiants.pencil.data.{
  Attachment,
  Body,
  Credentials,
  Email,
  From,
  Mailbox,
  Password,
  Subject,
  To,
  Username
}
import com.minosiants.pencil.protocol.Replies
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
import nl.surf.rdx.common.email.conf.EmailConf
import org.typelevel.log4cats.Logger
import cats.implicits._
import java.time.format.DateTimeFormatter

object RdxEmail {

  type Template[F[_], -V[*[_]]] = Kleisli[F, (EmailConf, V[F]), RdxEmail]
  type Sealed[F[_]] = Kleisli[F, EmailConf, RdxEmail]

  object Template {

    type NoVars[`?`[_]] = Unit

    def apply[F[_], V[*[_]]](tpl: ((EmailConf, V[F])) => F[RdxEmail]): Template[F, V] =
      Kleisli[F, (EmailConf, V[F]), RdxEmail](tpl)

    object syntax {
      implicit class TemplateOps[F[_], V[*[_]]](
          val template: Kleisli[F, (EmailConf, V[F]), RdxEmail]
      ) extends AnyVal {
        def seal(vars: V[F]): Sealed[F] =
          template.local[EmailConf]((_, vars))
      }
      implicit class NoVarsTemplateOps[F[_]](
          val template: Kleisli[F, (EmailConf, Unit), RdxEmail]
      ) extends AnyVal {
        def seal: Kleisli[F, EmailConf, RdxEmail] =
          template.local[EmailConf]((_, ()))
      }
    }

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm Z")
    val `<BODY>` = """<body style="font-size: 16px; font-family: Arial, sans-serif, 'Open Sans'">"""
    val `</BODY>` = """<br/></body>"""
  }

}

/**
  * This class has mostly the same attributes as [[Email]],
  * besides [[com.minosiants.pencil.data.From]] as it's
  * filled in via DI mechanism from the email configuration.
  */
case class RdxEmail(to: To, subject: Subject, body: Body, attachments: List[Attachment] = Nil)
