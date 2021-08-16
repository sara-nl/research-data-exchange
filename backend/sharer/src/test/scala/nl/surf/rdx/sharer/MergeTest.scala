package nl.surf.rdx.sharer

import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.Merge.Result
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.Observation
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MergeTest extends AnyFunSuite with Matchers {

  test("No stored shares and no observed shares => no changes") {
    Merge(Set.empty, Set.empty) shouldEqual Result(Set.empty, Set.empty)
  }

  test("No stored shares and some observed shares => added shares") {
    val observed = Set(
      OwncloudShare("id1", "mike", None, "/1", "file", 0),
      OwncloudShare("id2", "mike", None, "/2", "file", 0),
      OwncloudShare("id3", "mike", None, "/3", "file", 0)
    ).map(Observation(_, List.empty[String]))
    Merge(Set.empty, observed) shouldEqual Result(Set.empty, observed)
  }

  test("No observed shares and some stored shares => removed shares") {
    val stored = Set(
      OwncloudShare("id1", "mike", None, "/1", "file", 0),
      OwncloudShare("id2", "mike", None, "/2", "file", 0),
      OwncloudShare("id3", "mike", None, "/3", "file", 0)
    )

    Merge(stored, Set.empty) shouldEqual Result(stored, Set.empty)
  }

  test("Identical stored and observed shares => no removed/added shares") {
    val stored = Set(
      OwncloudShare("id1", "mike", None, "/1", "folder", 0),
      OwncloudShare("id2", "mike", None, "/2", "folder", 0),
      OwncloudShare("id3", "mike", None, "/3", "folder", 0)
    )

    val observed = stored.map(Observation(_, List.empty[String]))
    Merge(stored, observed) shouldEqual Result(removed = Set.empty, added = Set.empty)
  }

  test("Slightly different observed and stored shares => some removed/added shares") {
    val share1 = OwncloudShare("id1", "mike", None, "/1", "folder", 0)
    val share2 = OwncloudShare("id2", "mike", None, "/2", "folder", 0)
    val share3 = OwncloudShare("id3", "mike", None, "/3", "folder", 0)
    val share4 = OwncloudShare("id4", "mike", None, "/4", "folder", 0)

    val stored = Set(share2, share3, share4)
    val observed = Set(share1, share2, share3).map(Observation(_, List.empty[String]))

    Merge(stored, observed) shouldEqual Result(
      removed = Set(share4),
      added = Set(Observation(share1, Nil))
    )
  }

}
