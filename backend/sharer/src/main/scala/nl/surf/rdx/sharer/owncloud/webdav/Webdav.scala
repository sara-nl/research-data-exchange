package nl.surf.rdx.sharer.owncloud.webdav

import cats.data.Kleisli
import cats.effect.{IO, Resource}
import com.github.sardine.{DavResource, Sardine, SardineFactory}
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf

object Webdav {

  case object implicits {
    implicit class DavResourcesWithFilter(rr: List[DavResource]) {
      def hasFileNamed(name: String): Boolean =
        rr.map(_.getPath)
          .exists(_.toLowerCase.endsWith(name))
    }
  }

  def makeSardine: Kleisli[Resource[IO, *], OwncloudConf, Sardine] =
    Kleisli { conf =>
      Resource.make(
        IO(SardineFactory.begin(conf.webdavUsername, conf.webdavPassword))
      )(sardine => IO(sardine.shutdown()))
    }

}
