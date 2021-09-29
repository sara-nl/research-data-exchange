package nl.surf.rdx.sharer.owncloud

import better.files.StringInterpolations
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import com.github.sardine.Sardine
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.Observation
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.nio.file.Paths
import java.nio.file.{Path => JPath}
import nl.surf.rdx.common.testutils.ConsoleLogger._

class OwncloudSharesObserverTest
    extends OwncloudSharesObserverFixtures[IO]
    with AsyncFlatSpecLike
    with AsyncIOSpec
    with Matchers
    with MockitoSugar {

  private implicit class StringsToPaths(paths: List[String]) {
    def asPaths: List[JPath] = paths.map(Paths.get(_))
  }

  "OC Share observer" should "return new shares when available" in {
    runWithDeps(Flow.Happy)(OwncloudSharesObserver.observe[IO]).asserting { observations =>
      observations shouldBe List(
        Observation(
          shareFolder("dataset1"),
          List("1.csv", "2.csv", "conditions.pdf").asPaths
        )
      )
    }
  }

  it should "return new shares when folder name contains a whitespace" in {
    runWithDeps(Flow.FolderName("dataset one"))(OwncloudSharesObserver.observe[IO]).asserting {
      observations =>
        observations shouldBe List(
          Observation(
            shareFolder("dataset one"),
            List("1.csv", "2.csv", "conditions.pdf").asPaths
          )
        )
    }
  }

  //TODO: implement as a part of "hardening"
  ignore should "retry when share listing failed with 502" in {
    val s1 =
      OwncloudShare("id1", "uid1", None, "conditions.pdf", OwncloudShare.itemTypeFolder, 0, 0)
    val files1 = List(
      "/remote.php/nonshib-webdav/dataset1/",
      "/remote.php/nonshib-webdav/dataset1/conditions.pdf"
    ).asPaths
    val deps = OwncloudSharesObserver.Deps[IO](
      Resource.pure[IO, Sardine](
        helpers.sardineFailFirstWithPaths("dataset1", files1)
      ),
      IO.pure(List(s1)),
      helpers.webdavBase,
      "conditions.pdf"
    )

    OwncloudSharesObserver.observe[IO].run(deps).asserting { observations =>
      observations shouldBe List(Observation(s1, files1))
    }
  }

  //TODO: investigate as a part of "hardening"
  ignore should "fail when share listing failed with unknown reason" in {
    val s1 =
      OwncloudShare("id1", "uid1", None, "conditions.pdf", OwncloudShare.itemTypeFolder, 0, 0)
    val kaboom = new RuntimeException("Kaboom!")
    val deps = OwncloudSharesObserver.Deps[IO](
      Resource.pure[IO, Sardine](
        helpers.sardineFail("dataset1", kaboom)
      ),
      IO.pure(List(s1)),
      helpers.webdavBase,
      "conditions.pdf"
    )

    OwncloudSharesObserver.observe[IO].run(deps).attempt.asserting { observationsEither =>
      observationsEither shouldBe Left(kaboom)
    }
  }

  it should "filter out shares that are not folders" in {
    runWithDeps(Flow.ShareIsNotFolder)(OwncloudSharesObserver.observe[IO]).asserting {
      observations =>
        observations shouldBe List.empty[Observation]
    }
  }

  it should "filter out shares that don't have conditions file" in {
    runWithDeps(Flow.NoCondtionsFile)(OwncloudSharesObserver.observe[IO]).asserting {
      observations =>
        observations shouldBe List.empty[Observation]
    }
  }

}
