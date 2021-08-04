package nl.surf.rdx.sharer

import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.owncloud.DexShares.Observation

object Merge {

  case class Result(
      removed: Set[OwncloudShare],
      added: Set[Observation]
  )

  def apply(
      stored: Set[OwncloudShare],
      observed: Set[Observation]
  ): Result = {
    Result(stored.diff(observed.map(_.share)), observed.filterNot(o => stored.contains(o.share)))
  }

}
