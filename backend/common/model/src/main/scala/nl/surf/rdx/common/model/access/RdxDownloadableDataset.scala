package nl.surf.rdx.common.model.access

import nl.surf.rdx.common.model.owncloud.OwncloudShare

case class RdxDownloadableDataset(owncloudShare: OwncloudShare, conditionsUrl: String)
