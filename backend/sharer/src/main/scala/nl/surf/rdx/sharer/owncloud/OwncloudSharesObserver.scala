package nl.surf.rdx.sharer.owncloud

import cats.{Monad, Parallel}
import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift, IO, Resource, Sync}
import com.github.sardine.{DavResource, Sardine}
import com.github.sardine.impl.SardineException
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.owncloud.webdav.Webdav.implicits._
import org.http4s.Status.NotFound
import cats.implicits._
import nl.surf.rdx.common.owncloud.conf.OwncloudConf.WebdavBase
import org.typelevel.log4cats.Logger

import java.nio.file.Path

object OwncloudSharesObserver {

  case class ObservedShare(share: OwncloudShare, topLevelFiles: List[String])

  case class Observation(share: OwncloudShare, files: List[Path])

  case class Deps[F[_]](
      sardineR: Resource[F, Sardine],
      getShares: F[List[OwncloudShare]],
      webdavBase: WebdavBase,
      conditionsFileName: String
  )

  /**
    * Fetches OC shares that match the requirements of DEX:
    *  - are folders;
    *  - contain conditions document;
    *  - can be re-shared.
    */
  def observe[F[_]: ContextShift: Monad: ConcurrentEffect: Logger: Parallel]
      : Kleisli[F, Deps[F], List[Observation]] =
    Kleisli { deps =>
      for {
        folderShares <- deps.getShares.map(_.filter(_.item_type === OwncloudShare.itemTypeFolder))
        sharesAndListings <- deps.sardineR.use(sardine =>
          folderShares.parTraverse { ocs =>
            OwncloudFiles
              .listTopLevel(ocs.path)
              .mapF(
                _.handleErrorWith(
                  {
                    case x: SardineException if x.getStatusCode === NotFound.code =>
                      Logger[F].warn(
                        s"Error response when listing ${ocs.path}: ${x.getStatusCode} ${x.getMessage}"
                      ) >>
                        Sync[F].pure(List.empty[DavResource])
                    case x =>
                      Logger[F].warn(s"Unknown error when listing ${ocs.path}") >>
                        Sync[F].raiseError[List[DavResource]](x)
                  }
                )
              )
              .map((ocs, _))
              .run((sardine, deps.webdavBase))
          }
        )
      } yield sharesAndListings.collect {
        case (ocs, davResources) if davResources.hasFileNamed(deps.conditionsFileName) =>
          import better.files.{File => BFile}
          val shareRoot = BFile(deps.webdavBase.serverSuffix, ocs.path)
          val userPaths = davResources.map(r => BFile(r.getPath)).map(shareRoot.relativize)
          Observation(ocs, userPaths)
      }
    }

}
