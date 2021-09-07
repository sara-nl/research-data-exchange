package nl.surf.rdx.sharer.email

import cats.data.Kleisli
import cats.effect.{Blocker, Concurrent, ContextShift, Sync}
import com.minosiants.pencil.Client
import com.minosiants.pencil.data.{Credentials, Email, Password, Username}
import com.minosiants.pencil.protocol.Replies
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
import nl.surf.rdx.sharer.conf.SharerConf.EmailConf
import org.typelevel.log4cats.Logger
import cats.implicits._

object RdxEmail {

  def send[F[_]: ContextShift: Logger: Sync: Concurrent](
      email: Email
  ): Kleisli[F, EmailConf, Replies] =
    Kleisli { conf =>
      Blocker[F]
        .use { blocker =>
          SocketGroup[F](blocker).use { sg =>
            TLSContext.system[F](blocker).flatMap { tls =>
              val client =
                Client[F](
                  conf.host,
                  conf.port,
                  (conf.user, conf.password).tupled.map {
                    case (u, p) => Credentials(Username(u), Password(p))
                  }
                )(
                  blocker,
                  sg,
                  tls,
                  Logger[F]
                )
              client.send(email)
            }
          }
        }
    }

}
