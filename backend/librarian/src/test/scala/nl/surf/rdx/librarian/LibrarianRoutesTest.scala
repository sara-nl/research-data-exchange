package nl.surf.rdx.librarian

import cats.effect.IO
import cats.implicits._
import nl.surf.rdx.common.model.{RdxDataset, RdxShare}
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.librarian.codecs.service.DatasetService
import org.http4s.implicits._
import org.http4s._
import org.mockito.ArgumentMatchers.any
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime
import org.mockito.{ArgumentMatchers, MockitoSugar}
import io.circe.literal._
import io.lemonlabs.uri.RelativeUrl
import nl.surf.rdx.common.model.api.UserMetadata
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.POST

import java.util.UUID

class LibrarianRoutesTest
    extends AnyFlatSpec
    with Matchers
    with MockitoSugar
    with LibrarianFixtures {
  import LibrarianRoutesTest._
  import LibrarianRoutes._

  "Get unpublished share route" should "return an available share" in withDataService4Share {
    dsService =>
      for {
        uri <- IO.fromEither(Uri.fromString(s"/share/${uuid.toString}"))
        request = Request[IO](Method.GET, uri)
        response <- getRdxShareRoute(dsService).orNotFound.run(request)
        _ <- IO(response.status shouldBe Status.Ok)
      } yield ()
  }

  it should "return 400 error for missing token" in withDataService4Share { dsService =>
    for {
      uri <- IO.fromEither(Uri.fromString(s"/share/"))
      request = Request[IO](Method.GET, uri)
      response <- getRdxShareRoute(dsService).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.BadRequest)
    } yield ()
  }

  it should "return 403 error for invalid token" in withDataService4Share { dsService =>
    for {
      uri <- IO.fromEither(Uri.fromString("/share/bla"))
      request = Request[IO](Method.GET, uri)
      response <- getRdxShareRoute(dsService).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Forbidden)
    } yield ()
  }

  it should "return 403 error for expired token" in withDataService4Share { dsService =>
    for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${expiredUuid.toString}"))
      request = Request[IO](Method.GET, uri)
      response <- getRdxShareRoute(dsService).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.Forbidden)
    } yield ()
  }

  it should "return 404 for unavailable share" in withDataService4Share { dsService =>
    for {
      uri <- IO.fromEither(Uri.fromString(s"/share/${UUID.randomUUID().toString}"))
      request = Request[IO](Method.GET, uri)
      response <- getRdxShareRoute(dsService).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.NotFound)
    } yield ()
  }

  "Publish " should "call dataset service and return created status" in withDataService4Publish {
    dsService =>
      import org.http4s.circe.CirceEntityEncoder._

      for {
        uri <- IO.fromEither(Uri.fromString(s"/dataset/${uuid.toString}"))
        request <- POST(publishJson, uri)
        response <- publishDatasetRoute(dsService).orNotFound.run(request)
        _ <- IO(response.status shouldBe Status.Created)
      } yield ()
  }

  it should "return error if error occurred while publishing" in withDataService4Publish {
    dsService =>
      import org.http4s.circe.CirceEntityEncoder._
      for {
        uri <- IO.fromEither(Uri.fromString(s"/dataset/${UUID.randomUUID().toString}"))
        request <- POST(publishJson, uri)
        response <- publishDatasetRoute(dsService).orNotFound.run(request)
        _ <- IO(response.status shouldBe Status.InternalServerError)
      } yield ()
  }

  it should "return error for malformed json" in withDataService4Publish { dsService =>
    import org.http4s.circe.CirceEntityEncoder._

    for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/${uuid.toString}"))
      request <- POST(notValidJson, uri)
      response <- publishDatasetRoute(dsService).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.BadRequest)
    } yield ()
  }

  "Load dataset details" should "return dataset details if doi is provided" in withDataService4Dataset {
    dsService =>
      for {
        uri <- IO.fromEither(Uri.fromString(s"/dataset/$doi"))
        request = Request[IO](Method.GET, uri)
        response <- getDatasetRoute(dsService).orNotFound.run(request)
        _ <- IO(response.status shouldBe Status.Ok)
      } yield ()
  }

  it should "return not found if doi is not provided" in withDataService4Dataset { dsService =>
    for {
      uri <- IO.fromEither(Uri.fromString(s"/dataset/"))
      request = Request[IO](Method.GET, uri)
      response <- getDatasetRoute(dsService).orNotFound.run(request)
      _ <- IO(response.status shouldBe Status.NotFound)
    } yield ()
  }
}

trait LibrarianFixtures extends MockitoSugar {
  val uuid: UUID = UUID.randomUUID()
  val expiredUuid: UUID = UUID.randomUUID()
  val doi: String = "10.1000%2Fthe.last.part"

  private val shareToken: RdxShare = RdxShare(
    OwncloudShare("id1", "user1", None, "/share1", "file", 10, 0),
    OffsetDateTime.now(),
    uuid.some,
    OffsetDateTime.now().plusDays(1),
    "sales@microsoft.com",
    List.empty,
    "http://example.com"
  )

  val expiredShareToken: RdxShare = RdxShare(
    OwncloudShare("id1", "user1", None, "/share1", "file", 10, 0),
    OffsetDateTime.now(),
    uuid.some,
    OffsetDateTime.now().minusDays(1),
    "sales@microsoft.com",
    List.empty,
    "http://example.com"
  )

  private[librarian] val publishedDataset: RdxDataset = RdxDataset(
    "title",
    "description",
    "http:/example.com/conditions.pdf",
    Seq("file.csv", "folder/")
  )

  protected def withDataService4Publish(test: DatasetService[IO] => IO[Unit]): Unit = {
    val dsMock = mock[DatasetService[IO]]
    when(dsMock.publishShare(any[UUID], any[UserMetadata]))
      .thenReturn(IO.raiseError(new RuntimeException("Oeps")))
    when(dsMock.publishShare(ArgumentMatchers.eq(uuid), any[UserMetadata])).thenReturn(IO.unit)
    val io = test(dsMock)
    io.unsafeRunSync()
  }

  protected def withDataService4Dataset(test: DatasetService[IO] => IO[Unit]): Unit = {
    val dsMock = mock[DatasetService[IO]]
    val doiUrl = RelativeUrl.parse("10.1000/the.last.part")
    when(dsMock.fetchDataset(any[RelativeUrl]))
      .thenReturn(IO.none)
    when(dsMock.fetchDataset(ArgumentMatchers.eq(doiUrl)))
      .thenReturn(IO(Some(publishedDataset)))

    val io = test(dsMock)
    io.unsafeRunSync()
  }

  protected def withDataService4Share(test: DatasetService[IO] => IO[Unit]): Unit = {
    val dsmock = mock[DatasetService[IO]]
    when(dsmock.fetchShare(any())).thenReturn(IO.none)
    when(dsmock.fetchShare(uuid)).thenReturn(IO.pure(shareToken.some))
    when(dsmock.fetchShare(expiredUuid)).thenReturn(IO.pure(expiredShareToken.some))
    val io = test(dsmock)
    io.unsafeRunSync()
  }

}
object LibrarianRoutesTest {
  val publishJson = json"""{
              "doi": "10.000/dsda",
              "title": "this is a title",
              "authors": "dada",  
              "description": "this is the description"
      }"""

  val notValidJson = json"""{"some": "value"}"""
}
