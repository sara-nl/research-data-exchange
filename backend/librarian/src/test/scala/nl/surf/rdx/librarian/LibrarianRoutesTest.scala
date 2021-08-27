package nl.surf.rdx.librarian

import cats.effect.IO
import cats.implicits._
import nl.surf.rdx.common.model.{ShareToken, UserMetadata}
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.librarian.LibrarianApp.Deps
import nl.surf.rdx.librarian.codecs.service.DatasetService
import nl.surf.rdx.librarian.conf.LibrarianConf
import org.http4s.implicits._
import org.http4s._
import org.mockito.ArgumentMatchers.any
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime
import org.mockito.{ArgumentMatchers, MockitoSugar}
import io.circe.literal._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.POST

import java.util.UUID

//todo extract common code
class LibrarianRoutesTest extends AnyFlatSpec with Matchers with MockitoSugar {
  import LibrarianRoutesTest._

  private val conf = LibrarianConf(80, "conditions.pdf")

  val deps: Deps = Deps(
    conf, {
      val dsmock = mock[DatasetService[IO]]
      when(dsmock.fetchShare(any())).thenReturn(IO.none)
      when(dsmock.fetchShare(uuid)).thenReturn(IO.pure(shareToken.some))
      when(dsmock.fetchShare(expiredUuid)).thenReturn(IO.pure(expiredShareToken.some))
      dsmock
    }
  )

  "Get unpublished share route" should "return an available share" in {
    val io = for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${uuid.toString}"))
      request = Request[IO](Method.GET, uri)
      response <- LibrarianRoutes.getUnpublishedShareRoute(deps).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Ok)
    } yield ()

    io.unsafeRunSync()
  }

  it should "return 400 error for missing token" in {
    val io = for {
      uri <- IO.fromEither(Uri.fromString(s"/share/"))
      request = Request[IO](Method.GET, uri)
      response <- LibrarianRoutes.getUnpublishedShareRoute(deps).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.BadRequest)
    } yield ()

    io.unsafeRunSync()
  }

  it should "return 403 error for invalid token" in {
    val io = for {
      uri <- IO.fromEither(Uri.fromString("/share/bla"))
      request = Request[IO](Method.GET, uri)
      response <- LibrarianRoutes.getUnpublishedShareRoute(deps).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Forbidden)
    } yield ()

    io.unsafeRunSync()
  }

  it should "return 403 error for expired token" in {
    val io = for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${expiredUuid.toString}"))
      request = Request[IO](Method.GET, uri)
      response <- LibrarianRoutes.getUnpublishedShareRoute(deps).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Forbidden)
    } yield ()

    io.unsafeRunSync()
  }

  it should "return 404 for unavailable share" in {
    val io = for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${UUID.randomUUID().toString}"))
      request = Request[IO](Method.GET, uri)
      response <- LibrarianRoutes.getUnpublishedShareRoute(deps).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.NotFound)
    } yield ()

    io.unsafeRunSync()
  }

  val dsService4Publish = {
    val dsMock = mock[DatasetService[IO]]
    when(dsMock.publishShare(any[UUID], any[UserMetadata]))
      .thenReturn(IO.raiseError(new RuntimeException("Oeps")))
    when(dsMock.publishShare(ArgumentMatchers.eq(uuid), any[UserMetadata])).thenReturn(IO.unit)
    dsMock
  }

  "Publish " should "call dataset service and return created status" in {
    import org.http4s.circe.CirceEntityEncoder._

    val io = for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/${uuid.toString}"))
      request <- POST(publishJson, uri)
      response <- LibrarianRoutes.publishDatasetRoute(dsService4Publish).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Created)
    } yield ()

    io.unsafeRunSync()
  }
  it should "return error if error occurred while publishing" in {
    import org.http4s.circe.CirceEntityEncoder._
    val random = UUID.randomUUID()
    val io = for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/${random.toString}"))
      request <- POST(publishJson, uri)
      response <- LibrarianRoutes.publishDatasetRoute(dsService4Publish).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.InternalServerError)
    } yield ()

    io.unsafeRunSync()
  }
  it should "return error for malformed json" in {
    import org.http4s.circe.CirceEntityEncoder._

    val io = for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/${uuid.toString}"))
      request <- POST(notValidJson, uri)
      response <- LibrarianRoutes.publishDatasetRoute(dsService4Publish).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.BadRequest)
    } yield ()

    io.unsafeRunSync()
  }

}

object LibrarianRoutesTest {

  val uuid: UUID = UUID.randomUUID()
  val expiredUuid: UUID = UUID.randomUUID()

  private val shareToken: ShareToken = ShareToken(
    OwncloudShare("id1", "user1", None, "/share1", "file", 10),
    OffsetDateTime.now(),
    uuid.some,
    OffsetDateTime.now().plusDays(1),
    "sales@microsoft.com",
    List.empty
  )

  val expiredShareToken: ShareToken = ShareToken(
    OwncloudShare("id1", "user1", None, "/share1", "file", 10),
    OffsetDateTime.now(),
    uuid.some,
    OffsetDateTime.now().minusDays(1),
    "sales@microsoft.com",
    List.empty
  )

  val publishJson = json"""{
              "doi": "10.000/dsda",
              "title": "this is a title",
              "authors": "dada",  
              "description": "this is the description"
      }"""

  val notValidJson = json"""{"some": "value"}"""
}
