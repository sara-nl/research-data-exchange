package nl.surf.rdx.common.model.api

import cats.effect.IO
import cats.effect.testing.scalatest.{AsyncIOSpec, EffectTestSupport}
import nl.surf.rdx.common.model.RdxShare
import nl.surf.rdx.common.model.api.ShareInfo.Deps
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class ShareInfoTest extends AsyncFlatSpec with Matchers with AsyncIOSpec with EffectTestSupport {

  private val conditions = "conditions.pdf"

  "separateConditions" should "remove conditions from file list" in {
    ShareInfo.separateConditions(
      List("conditions.pdf", "file 1", "file2"),
      conditions
    ) shouldBe (Some(
      "conditions.pdf"
    ), List("file 1", "file2"))
    ShareInfo.separateConditions(
      List("some/path/conditions.pdf", "file 1", "file2"),
      conditions
    ) shouldBe (Some(
      "some/path/conditions.pdf"
    ), List("file 1", "file2"))
    ShareInfo.separateConditions(
      List("Conditions.pdf", "file 1", "file2"),
      conditions
    ) shouldBe (Some(
      "Conditions.pdf"
    ), List("file 1", "file2"))
    ShareInfo.separateConditions(
      List("CONDITIONS.pdf", "file 1", "file2"),
      conditions
    ) shouldBe (Some(
      "CONDITIONS.pdf"
    ), List("file 1", "file2"))
    ShareInfo.separateConditions(List("file 1", "file2"), conditions) shouldBe (None, List(
      "file 1",
      "file2"
    ))
    ShareInfo.separateConditions(List("conditions.pdf"), conditions) shouldBe (Some(
      "conditions.pdf"
    ), List.empty)
    ShareInfo.separateConditions(List(), conditions) shouldBe (None, List.empty)
  }

  private val deps = Deps(conditions)
  private val baseRdxShare = RdxShare(
    OwncloudShare("", "", None, "", "", 1, 1),
    OffsetDateTime.now(),
    None,
    OffsetDateTime.now(),
    "",
    List(),
    ""
  )
  "fromShare" should "separate list of files" in {
    val files = List("conditions.pdf", "file 1", "file2")
    val rdxShare = baseRdxShare.copy(files = files)

    for {
      fromShare <- ShareInfo.fromShare[IO].run(deps)
      shareInfo <- fromShare(rdxShare)
    } yield {
      shareInfo.files shouldBe List("file 1", "file2")
      shareInfo.conditionsDocument shouldBe "conditions.pdf"
    }
  }

  it should "throw RuntimeException when conditions document is not present" in {
    val files = List("file 1", "file2")
    val rdxShare = baseRdxShare.copy(files = files)
    (for {
      fromShare <- ShareInfo.fromShare[IO].run(deps)
      _ <- fromShare(rdxShare)
    } yield ()).assertThrows[RuntimeException]

  }
}
