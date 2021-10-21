package nl.surf.rdx.common.email

import cats.Parallel
import cats.data.Kleisli
import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync}
import nl.surf.rdx.common.email.pencil.Client
//import com.minosiants.pencil.Client
import com.minosiants.pencil.data._
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
import nl.surf.rdx.common.email.RdxEmail.Sealed
import nl.surf.rdx.common.email.conf.EmailConf
import org.typelevel.log4cats.Logger
import cats.implicits._
import com.minosiants.pencil.protocol.Replies
import nl.surf.rdx.common.email.RdxEmailService.Deps

object RdxEmailService {
  case class Deps[F[_]](conf: EmailConf, clientR: Resource[F, Client[F]])

  object Deps {
    def clientR[F[_]: Sync: ContextShift: Logger: Concurrent](
        conf: EmailConf
    ): Resource[F, Client[F]] =
      for {
        blocker <- Blocker[F]
        sGroup <- SocketGroup[F](blocker)
        system <- Resource.eval(TLSContext.system[F](blocker))
      } yield Client[F](
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
  }

  def apply[F[_]: ContextShift: Logger: Sync: Concurrent: Parallel]()
      : Kleisli[F, Deps[F], RdxEmailService[F]] =
    Kleisli(new RdxEmailService[F](_).pure[F])
}

class RdxEmailService[F[_]: ContextShift: Logger: Sync: Concurrent: Parallel](deps: Deps[F]) {

  def sendMany(templates: List[Sealed[F]]): F[List[Replies]] =
    templates.map(send).parSequence

  def send(template: Sealed[F]): F[Replies] =
    deps.clientR.use { client =>
      for {
        rdxEmail <- template.run(deps.conf)
        from <- Sync[F].fromEither(Mailbox.fromString(deps.conf.from))
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
