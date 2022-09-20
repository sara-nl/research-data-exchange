package nl.surf.rdx.sharer.owncloud.webdav

import cats.data.Kleisli
import cats.effect.{IO, Resource, Sync}
import com.github.sardine.{DavResource, Sardine, SardineFactory}
import nl.surf.rdx.common.owncloud.conf.OwncloudConf

object Webdav {

  case object implicits {
    implicit class DavResourcesWithFilter(rr: List[DavResource]) {
      def hasFileNamed(name: String): Boolean =
        rr.map(_.getPath)
          .exists(_.toLowerCase.endsWith(name))
    }
  }

  def makeSardine[F[_]: Sync]: Kleisli[Resource[F, *], OwncloudConf, Sardine] =
    Kleisli { conf =>
      Resource.make(
        Sync[F].delay(SardineFactory.begin(conf.webdavUsername, conf.webdavPassword))
      )(sardine => Sync[F].delay(sardine.shutdown()))
    }

}
