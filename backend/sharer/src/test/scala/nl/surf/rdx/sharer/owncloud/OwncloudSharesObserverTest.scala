package nl.surf.rdx.sharer.owncloud

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.github.sardine.impl.SardineException
import com.github.sardine.{DavResource, Sardine}
import io.lemonlabs.uri.Url
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.SharerApp
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.Observation
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf.WebdavBase
import org.http4s.implicits.http4sLiteralsSyntax
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util
import scala.jdk.CollectionConverters._

class OwncloudSharesObserverTest
    extends AsyncFlatSpecLike
    with AsyncIOSpec
    with Matchers
    with MockitoSugar {

  implicit private val logger = Slf4jLogger.getLogger[IO]

  object helpers {

//    0 = {DavResource@8967} "/remote.php/nonshib-webdav/ds3/"
//    1 = {DavResource@8968} "/remote.php/nonshib-webdav/ds3/conditions.pdf"
//    2 = {DavResource@8969} "/remote.php/nonshib-webdav/ds3/f1/"
//    3 = {DavResource@8970} "/remote.php/nonshib-webdav/ds3/sensitive-data.csv"

    def webdavApiURL(userPath: String) =
      s"https://owncloud.server/remote.php/nonshib-webdav/$userPath"

    def resources(files: List[String]): util.List[DavResource] =
      files
        .map(path => when(mock[DavResource].getPath).thenReturn(path).getMock[DavResource])
        .asJava
    def sardineWithPaths(userPath: String, internals: List[String]): Sardine = {
      val rr = resources(internals)
      when(mock[Sardine].list(webdavApiURL(userPath), 1))
        .thenReturn(rr)
        .getMock[Sardine]
    }

    def sardineFailFirstWithPaths(userPath: String, internals: List[String]): Sardine = {
      val rr = resources(internals)
      when(mock[Sardine].list(webdavApiURL(userPath), 1))
        .thenThrow(new SardineException("Oh no!", 502, "Server failed to respond"))
        .andThen(rr)
        .getMock[Sardine]
    }

    def sardineFail(userPath: String, e: Throwable): Sardine = {
      when(mock[Sardine].list(webdavApiURL(userPath), 1))
        .thenThrow(e)
        .getMock[Sardine]
    }
  }

  "OC Share observer" should "return new shares when available" in {
    val s1 = OwncloudShare("id1", "uid1", None, "/dataset1", OwncloudShare.itemTypeFolder, 0, 0)
    val files1 = List(
      "/remote.php/nonshib-webdav/dataset1/",
      "/remote.php/nonshib-webdav/dataset1/1.csv",
      "/remote.php/nonshib-webdav/dataset1/2.csv",
      "/remote.php/nonshib-webdav/dataset1/conditions.pdf"
    )
    val deps = OwncloudSharesObserver.Deps[IO](
      Resource.pure[IO, Sardine](
        helpers.sardineWithPaths("dataset1", files1)
      ),
      IO.pure(List(s1)),
      WebdavBase("https://owncloud.server", "/remote.php/nonshib-webdav/"),
      "conditions.pdf"
    )

    OwncloudSharesObserver.observe[IO].run(deps).asserting { observations =>
      observations shouldBe List(
        Observation(
          s1,
          List(
            "1.csv",
            "2.csv",
            "conditions.pdf"
          )
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
    )
    val deps = OwncloudSharesObserver.Deps[IO](
      Resource.pure[IO, Sardine](
        helpers.sardineFailFirstWithPaths("dataset1", files1)
      ),
      IO.pure(List(s1)),
      WebdavBase("https://owncloud.server", "/remote.php/nonshib-webdav/"),
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
    val files1 = List(
      "/remote.php/nonshib-webdav/dataset1/",
      "/remote.php/nonshib-webdav/dataset1/conditions.pdf"
    )
    val kaboom = new RuntimeException("Kaboom!")
    val deps = OwncloudSharesObserver.Deps[IO](
      Resource.pure[IO, Sardine](
        helpers.sardineFail("dataset1", kaboom)
      ),
      IO.pure(List(s1)),
      WebdavBase("https://owncloud.server", "/remote.php/nonshib-webdav/"),
      "conditions.pdf"
    )

    OwncloudSharesObserver.observe[IO].run(deps).attempt.asserting { observationsEither =>
      observationsEither shouldBe Left(kaboom)
    }
  }

  it should "filter out shares that are not folders" in {
    val s1 = OwncloudShare("id1", "uid1", None, "conditions.pdf", OwncloudShare.itemTypeFile, 0, 0)
    val files1 = List(
      "/remote.php/nonshib-webdav/dataset1/",
      "/remote.php/nonshib-webdav/dataset1/conditions.pdf"
    )
    val deps = OwncloudSharesObserver.Deps[IO](
      Resource.pure[IO, Sardine](
        helpers.sardineWithPaths("dataset1", files1)
      ),
      IO.pure(List(s1)),
      WebdavBase("https://owncloud.server", "/remote.php/nonshib-webdav/"),
      "conditions.pdf"
    )

    OwncloudSharesObserver.observe[IO].run(deps).asserting { observations =>
      observations shouldBe List.empty[Observation]
    }
  }

  it should "filter out shares that don't have conditions file" in {
    val s1 = OwncloudShare("id1", "uid1", None, "dataset1", OwncloudShare.itemTypeFolder, 0, 0)
    val files1 =
      List(
        "/remote.php/nonshib-webdav/dataset1/",
        "/remote.php/nonshib-webdav/dataset1/1.csv",
        "/remote.php/nonshib-webdav/dataset1/2.csv"
      )
    val deps = OwncloudSharesObserver.Deps[IO](
      Resource.pure[IO, Sardine](
        helpers.sardineWithPaths("dataset1", files1)
      ),
      IO.pure(List(s1)),
      WebdavBase("https://owncloud.server", "/remote.php/nonshib-webdav/"),
      "conditions.pdf"
    )

    OwncloudSharesObserver.observe[IO].run(deps).asserting { observations =>
      observations shouldBe List.empty[Observation]
    }
  }

//  "OC Shares" should "return top level shares with normalized paths" in {
//    val s1 = OwncloudShare("id1", "uid1", None, "dataset1", OwncloudShare.itemTypeFolder, 0)
//    val files1 = List(
//      "/remote.php/nonshib-webdav/dataset1/",
//      "/remote.php/nonshib-webdav/dataset1/f1/",
//      "/remote.php/nonshib-webdav/dataset1/conditions.pdf",
//      "/remote.php/nonshib-webdav/dataset1/sensitive-data.csv"
//    )
//
//    OwncloudShares
//      .listTopLevel[IO]("ds3")
//      .run(
//        (
//          helpers.sardineWithPaths("ds3", files1),
//          WebdavBase("https://owncloud.server", "/remote.php/nonshib-webdav/")
//        )
//      )
//      .asserting { observations =>
//        observations shouldBe List(
//          Observation(
//            s1,
//            List(
//              "ds3/f1/",
//              "conditions.pdf",
//              "sensitive-data.csv"
//            )
//          )
//        )
//      }
//  }

}
