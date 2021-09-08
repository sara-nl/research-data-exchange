package nl.surf.rdx.sharer

import cats.effect.IO
import cats.effect.testing.scalatest.{AsyncIOSpec, EffectTestSupport}
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.common.testutils
import nl.surf.rdx.sharer.SharePipes.Result
import nl.surf.rdx.sharer.SharerApp.EnvF
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.Observation
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockitoSugar
import org.scalatest.funspec.AsyncFunSpecLike
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
class SharePipesTest
    extends AsyncFunSpecLike
    with AsyncIOSpec
    with Matchers
    with MockitoSugar
    with EffectTestSupport {

  describe("onlyElegible pipe") {

    val sharesFixture =
      List(
        Observation(
          OwncloudShare("test0", "test", None, "/test_folder", OwncloudShare.itemTypeFolder, 0, 16),
          List("conditions.pdf")
        ),
        Observation(
          OwncloudShare("test1", "test", None, "/test_file", OwncloudShare.itemTypeFile, 0, 16),
          List("conditions.pdf")
        ),
        Observation(
          OwncloudShare("test2", "test", None, "/test_folder", OwncloudShare.itemTypeFolder, 0, 16),
          List.empty
        ),
        Observation(
          OwncloudShare(
            "test3",
            "test",
            None,
            "/test_folder_not_reshareble",
            OwncloudShare.itemTypeFolder,
            0,
            1
          ),
          List("conditions.pdf")
        )
      )
    def testObservations(implicit l: Logger[IO]) =
      SharerConf
        .loadF[IO]
        .flatMap(conf =>
          fs2
            .Stream(sharesFixture)
            .covary[EnvF[IO, *]]
            .through(SharePipes.onlyElegible)
            .compile
            .toList
            .run(SharerApp.Deps(conf))
        )

    it("should let good shares through") {
      import nl.surf.rdx.common.testutils.ConsoleLogger._
      testObservations
        .asserting { observations =>
          observations.flatten.map(_.share.id) should contain("test0")
        }
    }

    it("filter out non-folders") {
      import testutils.ConsoleLogger._
      testObservations
        .asserting { observations =>
          observations.flatten.map(_.share.id) shouldNot contain("test1")
        }
    }

    it("log condition evaluation result") {
      val loggerMock = mock[Logger[IO]]
      when(loggerMock.debug(anyString())).thenReturn(IO.unit)
      when(loggerMock.info(anyString())).thenReturn(IO.unit)
      when(loggerMock.warn(anyString())).thenReturn(IO.unit)
      when(loggerMock.error(anyString())).thenReturn(IO.unit)

      // Passing the mock explicitly
      testObservations(loggerMock)
        .flatMap { _ =>
          IO({
            verify(loggerMock, times(1)).debug("Share {id: test0} passes RDX criteria")
            verify(loggerMock, times(1))
              .warn("Share {id: test1} is ignored because it's not a folder")
            verify(loggerMock, times(1))
              .warn("Share {id: test2} is ignored because it doesn't have conditions file")
            verify(loggerMock, times(1))
              .warn("Share {id: test3} doesn't grant necessary re-sharing permissions")
          }).assertNoException
        }
    }

    it("filter out shares without conditions file") {
      import testutils.ConsoleLogger._
      testObservations
        .asserting { observations =>
          observations.flatten.map(_.share.id) shouldNot contain("test2")
        }
    }

    it("filter out non-shareable folders") {
      import testutils.ConsoleLogger._
      testObservations
        .asserting { observations =>
          observations.flatten.map(_.share.id) shouldNot contain("test3")
        }
    }
  }

  describe("diff") {
    it("should find no changes when no stored shares and no observed shares") {
      SharePipes.diff(Set.empty, Set.empty) shouldEqual Result(Set.empty, Set.empty)
    }

    it("should find added shares when no stored shares and some observed shares") {
      val observed = Set(
        OwncloudShare("id1", "mike", None, "/1", "file", 0, 0),
        OwncloudShare("id2", "mike", None, "/2", "file", 0, 0),
        OwncloudShare("id3", "mike", None, "/3", "file", 0, 0)
      ).map(Observation(_, List.empty[String]))
      SharePipes.diff(Set.empty, observed) shouldEqual Result(Set.empty, observed)
    }

    it("should find removed shares when observed shares and some stored shares") {
      val stored = Set(
        OwncloudShare("id1", "mike", None, "/1", "file", 0, 0),
        OwncloudShare("id2", "mike", None, "/2", "file", 0, 0),
        OwncloudShare("id3", "mike", None, "/3", "file", 0, 0)
      )

      SharePipes.diff(stored, Set.empty) shouldEqual Result(stored, Set.empty)
    }

    it("Identical stored and observed shares => no removed/added shares") {
      val stored = Set(
        OwncloudShare("id1", "mike", None, "/1", "folder", 0, 0),
        OwncloudShare("id2", "mike", None, "/2", "folder", 0, 0),
        OwncloudShare("id3", "mike", None, "/3", "folder", 0, 0)
      )

      val observed = stored.map(Observation(_, List.empty[String]))
      SharePipes.diff(stored, observed) shouldEqual Result(removed = Set.empty, added = Set.empty)
    }

    it("Slightly different observed and stored shares => some removed/added shares") {
      val share1 = OwncloudShare("id1", "mike", None, "/1", "folder", 0, 0)
      val share2 = OwncloudShare("id2", "mike", None, "/2", "folder", 0, 0)
      val share3 = OwncloudShare("id3", "mike", None, "/3", "folder", 0, 0)
      val share4 = OwncloudShare("id4", "mike", None, "/4", "folder", 0, 0)

      val stored = Set(share2, share3, share4)
      val observed = Set(share1, share2, share3).map(Observation(_, List.empty[String]))

      SharePipes.diff(stored, observed) shouldEqual Result(
        removed = Set(share4),
        added = Set(Observation(share1, Nil))
      )
    }
  }

}
