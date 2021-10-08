package nl.surf.rdx.common.email

import cats.effect.{IO, Resource, Sync, Timer}
import cats.effect.testing.scalatest.{AsyncIOSpec, EffectTestSupport}
import com.minosiants.pencil.Client
import com.minosiants.pencil.data.{Body, Email, Mailbox, Subject, To}
import com.minosiants.pencil.protocol.{Code, Replies, Reply}
import nl.surf.rdx.common.email.conf.EmailConf
import nl.surf.rdx.common.testutils.Fixtures
import org.mockito.{MockitoSugar, ArgumentMatchers => mm}
import cats.implicits._

import scala.concurrent.duration.FiniteDuration

trait RdxEmailServiceTestFxt {
  this: AsyncIOSpec with MockitoSugar =>

  sealed trait Flow
  object Flow {
    case object Happy extends Flow
    case class DelayedSend(delay: FiniteDuration) extends Flow
  }

  type Fxt = (RdxEmail.Template[IO, TestTemplateVars], Client[IO], RdxEmailService.Deps[IO])

  case class TestTemplateVars[F[_]](to: String)
  private val emailTemplate =
    RdxEmail.Template[IO, TestTemplateVars] {
      case (_, TestTemplateVars(to)) =>
        RdxEmail(
          To(Mailbox.unsafeFromString(to)),
          Subject(s"Test subject"),
          Body.Ascii("Hello World!")
        ).pure[IO]
    }

  private val successReplies = Replies(Reply(Code.`220`, "", ""))

  val fixtures: Fixtures[IO, Flow, Fxt] =
    Fixtures[IO, Flow, Fxt] {

      case Flow.`Happy` =>
        val pencilClient = mock[Client[IO]]
        when(pencilClient.send(mm.any[Email]))
          .thenReturn(successReplies.pure[IO])

        for {
          conf <- EmailConf.loadF[IO]
        } yield (
          emailTemplate,
          pencilClient,
          RdxEmailService.Deps[IO](conf, pencilClient.pure[Resource[IO, *]])
        )

      case Flow.DelayedSend(delay) =>
        val pencilClient = mock[Client[IO]]
        when(pencilClient.send(mm.any[Email]))
          .thenReturn(Timer[IO].sleep(delay) >> successReplies.pure[IO])
        for {
          conf <- EmailConf.loadF[IO]
        } yield (
          emailTemplate,
          pencilClient,
          RdxEmailService.Deps[IO](conf, pencilClient.pure[Resource[IO, *]])
        )

    }
}
