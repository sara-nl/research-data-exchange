package nl.surf.rdx.sharer.owncloud.conf

import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf.WebdavBase
import org.http4s.blaze.http.Url

case class OwncloudConf(
    webdavUsername: String,
    webdavPassword: String,
    sharesSource: Url,
    maxFolderDepth: Short,
    webdavBase: WebdavBase,
    minimumPermissionLevel: Byte
)

object OwncloudConf {

  case class WebdavBase(serverUri: Url, serverSuffix: String)

}
