package nl.surf.rdx

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits.catsSyntaxFlatMapOps
import com.minosiants.pencil.data.{Body, Email}
import com.minosiants.pencil._
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
import org.typelevel.log4cats.slf4j.Slf4jLogger

object DemoApp extends IOApp.Simple {
  val run = {

    val email = Email.text(
      from"rdx@surf.nl",
      to"mike.kotsur@surf.nl",
      subject"Server started",
      Body.Ascii("hello, server has started")
    )

    val logger = Slf4jLogger.getLogger[IO]

    val emailIO = Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          TLSContext.system[IO](blocker).flatMap { tls =>
            val client = Client[IO]("mh.surfsara.nl", 25)(blocker, sg, tls, logger)
            client.send(email)
          }
        }
      }

    IO(println("Hello World 0.1!")) >> emailIO >> IO.never
  }
}
