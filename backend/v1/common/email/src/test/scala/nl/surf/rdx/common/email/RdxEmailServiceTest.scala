package nl.surf.rdx.common.email
import cats.effect.testing.scalatest.{AsyncIOSpec, EffectTestSupport}
import cats.effect.{Clock, IO}
import cats.implicits._
import com.minosiants.pencil.data.Email
import com.minosiants.pencil.protocol.{Code, Replies, Reply}
import nl.surf.rdx.common.testutils.ConsoleLogger
import org.mockito.{MockitoSugar, ArgumentMatchers => mm}
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

class RdxEmailServiceTest
    extends AsyncFlatSpecLike
    with AsyncIOSpec
    with Matchers
    with EffectTestSupport
    with MockitoSugar
    with RdxEmailServiceTestFxt {

  "RDX Email" should "send a single email" in fixtures.withFxt(Flow.Happy) {
    case (testTemplate, clientMock, deps) =>
      import ConsoleLogger._
      import RdxEmail.Template.syntax._
      for {
        replies <- new RdxEmailService[IO](deps)
          .send(testTemplate.seal(TestTemplateVars("hello@example.com")))
        _ <- IO {
          verify(clientMock).send(
            mm.argThat[Email](_.to.boxes.head.address eqv "hello@example.com")
          )
          replies shouldEqual Replies(Reply(Code.`220`, "", ""))
        }
      } yield ()
  }

  "RDX Email" should "send many emails" in fixtures.withFxt(Flow.Happy) {
    case (testTemplate, clientMock, depx) =>
      import ConsoleLogger._
      import RdxEmail.Template.syntax._

      val email1 = testTemplate.seal(TestTemplateVars("hello1@example.com"))
      val email2 = testTemplate.seal(TestTemplateVars("hello2@example.com"))

      for {
        replies <- new RdxEmailService[IO](depx).sendMany(List(email1, email2))
        _ <- IO {
          verify(clientMock).send(
            mm.argThat[Email](_.to.boxes.head.address eqv "hello1@example.com")
          )
          verify(clientMock).send(
            mm.argThat[Email](_.to.boxes.head.address eqv "hello2@example.com")
          )
          replies shouldEqual List(
            Replies(Reply(Code.`220`, "", "")),
            Replies(Reply(Code.`220`, "", ""))
          )
        }
      } yield ()
  }

  "RDX Email" should "send many emails fast" in fixtures.withFxt(Flow.DelayedSend(300.millis)) {
    case (testTemplate, _, deps) =>
      import ConsoleLogger._
      import RdxEmail.Template.syntax._

      val tpl1 = testTemplate.seal(TestTemplateVars("hello1@example.com"))
      val tpl2 = testTemplate.seal(TestTemplateVars("hello2@example.com"))

      for {
        timeStart <- Clock[IO].realTime(TimeUnit.MILLISECONDS)
        _ <- new RdxEmailService[IO](deps).sendMany(List(tpl1, tpl2))
        timeFinish <- Clock[IO].realTime(TimeUnit.MILLISECONDS)
        _ <- IO {
          (timeFinish - timeStart) should be < 600L
        }
      } yield ()
  }
}
