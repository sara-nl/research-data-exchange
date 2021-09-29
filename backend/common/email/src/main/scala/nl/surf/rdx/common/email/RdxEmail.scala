package nl.surf.rdx.common.email

import cats.Monad
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

  type Template[F[_], V[*[_]]] = Kleisli[F, (EmailConf, V[F]), RdxEmail]
  type SendTemplate[F[_]] = Kleisli[F, EmailConf, RdxEmail]
  type SendMail[F[_]] = SendTemplate[F] => F[Replies]

  object Template {
    def apply[F[_], V[*[_]]](tpl: ((EmailConf, V[F])) => F[RdxEmail]): Template[F, V] =
      Kleisli[F, (EmailConf, V[F]), RdxEmail](tpl)

    object implicits {
      implicit class TemplateOps[F[_], V[*[_]]](
          val template: Kleisli[F, (EmailConf, V[F]), RdxEmail]
      ) extends AnyVal {
        def resolveVars(vars: V[F]): Kleisli[F, EmailConf, RdxEmail] =
          template.local[EmailConf]((_, vars))
      }
    }

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm Z")
    val `<BODY>` = """<body style="font-size: 16px; font-family: Arial, sans-serif, 'Open Sans'">"""
    val `</BODY>` = """</body>"""
  }

  def send[F[_]: ContextShift: Logger: Sync: Concurrent]: Kleisli[F, EmailConf, SendMail[F]] =
    Kleisli.fromFunction { conf => template =>
      val helpers = for {
        blocker <- Blocker[F]
        sGroup <- SocketGroup[F](blocker)
      } yield (blocker, sGroup)

      helpers.use {
        case (blocker, sGroup) =>
          for {
            system <- TLSContext.system[F](blocker)
            client = Client[F](
              conf.host,
              conf.port,
              (conf.user.map(Username(_)), conf.password.map(Password(_))).tupled
                .map((Credentials.apply _).tupled)
            )(
              blocker,
              sGroup,
              system,
              Logger[F]
            )
            rdxEmail <- template.run(conf)
            from <- Sync[F].fromEither(Mailbox.fromString(conf.from))
            res <- client.send(
              Email
                .mime(
                  From(from),
                  rdxEmail.to,
                  rdxEmail.subject,
                  rdxEmail.body
                )
                .copy(attachments = rdxEmail.attachments)
            )
          } yield res

      }
    }

}

/**
  * This class has mostly the same attributes as [[Email]],
  * besides [[com.minosiants.pencil.data.From]] as it's
  * filled in via DI mechanism from the email configuration.
  */
case class RdxEmail(to: To, subject: Subject, body: Body, attachments: List[Attachment] = Nil)
