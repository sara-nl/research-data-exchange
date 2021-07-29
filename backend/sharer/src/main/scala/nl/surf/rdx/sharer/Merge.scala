package nl.surf.rdx.sharer

import nl.surf.rdx.common.model.owncloud.OwncloudShare

object Merge {

  case class Result(
      removed: Set[OwncloudShare],
      added: Set[OwncloudShare],
      merged: Set[OwncloudShare]
  )

  def apply(
      stored: Set[OwncloudShare],
      observed: Set[OwncloudShare]
  ): Result = {
    val removed = stored.diff(observed)
    val added = observed.diff(stored)
    Result(removed, added, merged = stored.diff(removed) ++ added)
  }

}
