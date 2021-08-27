package nl.surf.rdx.librarian.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import UnpublishedShare._
import io.circe.syntax.EncoderOps

import java.time.OffsetDateTime
class UnpublishedShareTest extends AnyFlatSpec with Matchers {

  val conditions = "conditions.pdf"

  "separateConditions" should "remove conditions from file list" in {
    separateConditions(List("conditions.pdf", "file 1", "file2"), conditions) shouldBe (Some(
      "conditions.pdf"
    ), List("file 1", "file2"))
    separateConditions(
      List("some/path/conditions.pdf", "file 1", "file2"),
      conditions
    ) shouldBe (Some(
      "some/path/conditions.pdf"
    ), List("file 1", "file2"))
    separateConditions(List("Conditions.pdf", "file 1", "file2"), conditions) shouldBe (Some(
      "Conditions.pdf"
    ), List("file 1", "file2"))
    separateConditions(List("CONDITIONS.pdf", "file 1", "file2"), conditions) shouldBe (Some(
      "CONDITIONS.pdf"
    ), List("file 1", "file2"))
    separateConditions(List("file 1", "file2"), conditions) shouldBe (None, List("file 1", "file2"))
    separateConditions(List("conditions.pdf"), conditions) shouldBe (Some(
      "conditions.pdf"
    ), List.empty)
    separateConditions(List(), conditions) shouldBe (None, List.empty)
  }

  "codecs" should "serialize correctly" in {
    import UnpublishedShare.codecs._
    val share = UnpublishedShare(
      Some("one@example.com"),
      "path",
      OffsetDateTime.now(),
      Some(conditions),
      List("file1.pdf", "creëren.pdf", "crêpe.xml")
    )
    val json = share.asJson
    json.hcursor.get[String]("path") shouldBe Right("path")
    json.hcursor.get[Option[String]]("owner") shouldBe Right(Some("one@example.com"))
    json.hcursor.get[Option[String]]("conditionsDocument") shouldBe Right(Some(conditions))
    json.hcursor.get[List[String]]("files") shouldBe
      Right(List("file1.pdf", "creëren.pdf", "crêpe.xml"))
  }

}
