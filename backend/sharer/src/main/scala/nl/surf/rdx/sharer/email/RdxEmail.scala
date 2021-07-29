package nl.surf.rdx.sharer.email

import cats.data.Kleisli
import cats.effect.{Blocker, ContextShift, IO}
import com.minosiants.pencil.Client
import com.minosiants.pencil.data.{Credentials, Email, Password, Username}
import com.minosiants.pencil.protocol.Replies
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
import nl.surf.rdx.sharer.conf.SharerConf.EmailConf
import org.typelevel.log4cats.Logger
import cats.implicits._

object RdxEmail {

  def send(
      email: Email
  )(implicit cs: ContextShift[IO], logger: Logger[IO]): Kleisli[IO, EmailConf, Replies] =
    Kleisli { conf =>
      Blocker[IO]
        .use { blocker =>
          SocketGroup[IO](blocker).use { sg =>
            TLSContext.system[IO](blocker).flatMap { tls =>
              val client =
                Client[IO](
                  conf.host,
                  conf.port,
                  (conf.user, conf.password).tupled.map {
                    case (u, p) => Credentials(Username(u), Password(p))
                  }
                )(
                  blocker,
                  sg,
                  tls,
                  logger
                )
              client.send(email)
            }
          }
        }
    }

}
