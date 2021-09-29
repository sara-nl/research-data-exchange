package nl.surf.rdx.librarian.routes

import cats.data.Kleisli
import cats.effect.IO
import cats.effect.testing.scalatest.EffectTestSupport
import cats.implicits._
import com.minosiants.pencil.data.Body.{Ascii, Html}
import com.minosiants.pencil.data.{Attachment, Email, Subject}
import io.circe.literal._
import nl.surf.rdx.common.email.RdxEmail
import nl.surf.rdx.common.model.api.UserMetadata
import nl.surf.rdx.common.testutils.ConsoleLogger
import org.http4s
import org.http4s._
import org.http4s.circe.CirceEntityEncoder
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.{GET, POST}
import org.http4s.implicits._
import org.mockito.ArgumentMatchers.argThat
import org.mockito.{MockitoSugar, ArgumentMatchers => matchers}
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class LibrarianRoutesTest
    extends LibrarianRoutesFixtures[IO]
    with AsyncFlatSpecLike
    with Matchers
    with EffectTestSupport
    with MockitoSugar {
  import LibrarianRoutesTest._

  "Get unpublished share route" should "return an available share" in {
    import ConsoleLogger._
    for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${uuid.toString}"))
      request = Request[IO](Method.GET, uri)
      routes <- runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Ok)
    } yield ()
  }

  it should "return 404 error for missing token" in {
    import ConsoleLogger._
    for {
      uri <- IO.fromEither(Uri.fromString(s"/share/"))
      request = GET(uri)
      routes <- runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.NotFound)
    } yield ()
  }

  it should "return 403 error for invalid token" in {
    import ConsoleLogger._
    for {
      uri <- IO.fromEither(Uri.fromString("/share/bla"))
      request = GET(uri)
      routes <- runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Forbidden)
    } yield ()
  }

  it should "return 403 error for expired token" in {
    import ConsoleLogger._
    for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${expiredUuid.toString}"))
      request = GET(uri)
      routes <- runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Forbidden)
    } yield ()
  }

  it should "return 404 for unavailable share" in {
    import ConsoleLogger._
    for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${UUID.randomUUID().toString}"))
      request = GET(uri)
      routes <- runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.NotFound)
    } yield ()
  }

  "Publish " should "call dataset service and return created status" in {
    import CirceEntityEncoder._
    import ConsoleLogger._

    for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/${uuid.toString}"))
      request = POST(publishJson, uri)
      routes <- runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Created)
    } yield ()
  }

  it should "return error if error occurred while publishing" in withDeps(Flow.Happy) { deps =>
    import CirceEntityEncoder._
    import ConsoleLogger._

    when(deps.ds.publishShare(matchers.eq(uuid), matchers.any[UserMetadata]))
      .thenReturn(IO.raiseError(new RuntimeException("Oeps")))

    for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/${uuid.toString}"))
      request = POST(publishJson, uri)
      routes <- LibrarianRoutes.all[IO].run(deps)
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.InternalServerError)
    } yield ()
  }

  it should "return error for malformed json" in {
    import CirceEntityEncoder._
    import ConsoleLogger._
    for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/${uuid.toString}"))
      request = POST(notValidJson, uri)
      routes <- runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.BadRequest)
    } yield ()
  }

  "Load dataset details" should "return dataset details if doi is provided" in {
    for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/$doiUrlEncoded"))
      routes <- {
        import ConsoleLogger._
        runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      }
      request = GET(uri)
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Ok)
    } yield ()
  }

  it should "return not found if doi is not provided" in {
    for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/"))
      routes <- {
        import ConsoleLogger._
        runWithDeps(Flow.Happy)(LibrarianRoutes.all[IO])
      }
      request = GET(uri)
      response <- routes.orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.NotFound)
    } yield ()
  }

  "Create access request" should "send email with generated access link" in {
    import ConsoleLogger._
    import cats.syntax.eq._
    import http4s.circe._
    for {
      uri <- IO.fromEither(Uri.fromString(s"/access/$doiUrlEncoded"))
      deps = depsFor(Flow.Happy)
      route <- LibrarianRoutes.all[IO].run(deps)
      request = POST(requestAccessJson, uri)
      response <- route.orNotFound.run(request)
      _ <- IO {
        response.status shouldBe Status.Ok
//        verify(deps.sendMail)(argThat[RdxEmail](_.to.boxes.head.address eqv "john@doe.com"))
//        verify(deps.sendMail)(argThat[RdxEmail](_.from.box.address eqv "rdx@surf.nl"))
//        verify(deps.sendMail)(
//          argThat[MimeEmail](
//            _.attachments contains Attachment(
//              Path.of(attachmentPath)
//            )
//          )
//        )
//        verify(deps.sendMail)(argThat[RdxEmail] {
//          case RdxEmail(_, _, Ascii(body), _) =>
//            body contains publicLink
//        })

//        verify(deps.sendMail)(argThat[RdxEmail] {
//          case RdxEmail(_, Subject(subj), _, _) =>
//            subj contains "dataset folder"
//        })
      }
    } yield ()
  }

  it should "return NotFound if there is no share for this DOI" in {
    runWithDeps(Flow.Happy) {
      Kleisli { deps =>
        import ConsoleLogger._
        import http4s.circe._
        for {
          uri <- IO.fromEither(Uri.fromString(s"/access/foo%2Fbar"))
          route <- LibrarianRoutes.all[IO].run(deps)
          request = POST(requestAccessJson, uri)
          response <- route.orNotFound.run(request)
          _ <- IO {
            response.status shouldBe Status.NotFound
          }
        } yield ()
      }
    }
  }
}

object LibrarianRoutesTest {
  val publishJson = json"""{
              "doi": "10.000/dsda",
              "title": "this is a title",
              "authors": "dada",  
              "description": "this is the description"
      }"""

  val requestAccessJson =
    json"""{
    "name": "John Doe",
    "email": "john@doe.com"
        }"""

  val notValidJson = json"""{"some": "value"}"""
}
