package nl.surf.rdx.sharer.owncloud.path

import java.net.URI

case class WebdavPath(serverUri: URI, serverSuffix: String, userPath: Option[String] = None)
