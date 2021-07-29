package nl.surf.rdx.sharer.owncloud

import cats.data.Kleisli
import cats.effect.{ContextShift, IO, Resource}
import com.github.sardine.Sardine
import io.lemonlabs.uri.typesafe.dsl.{pathPartToUrlDsl, urlToUrlDsl}
import nl.surf.rdx.sharer.owncloud.conf.OwncloudConf.WebdavBase
import nl.surf.rdx.sharer.owncloud.webdav.Webdav
import cats.implicits._
import com.github.sardine.impl.SardineException
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.webdav.Webdav.implicits._
import org.http4s.Status.NotFound

object DexShares {

  case class Deps(sharesDeps: OwncloudShares.Deps, conf: SharerConf)

  /**
    * Fetches recent DEX shares (i.e. shares that are folders and contain conditions document).
    */
  def observe(implicit ec: ContextShift[IO]): Kleisli[IO, Deps, List[OwncloudShare]] =
    (for {
      allShares <-
        OwncloudShares.getShares
          .local[Deps](_.sharesDeps)
          .mapF[Resource[IO, *], List[OwncloudShare]](Resource.eval)
      sardineR <- Webdav.makeSardine.local[Deps](_.conf.owncloud) // [Resource, Deps, Sardine]
      dexShares <- doObserve(allShares)
        .mapF[Resource[IO, *], List[OwncloudShare]](Resource.eval)
        .local[Deps] { case Deps(_, conf) => (sardineR, conf) }
    } yield dexShares).mapF(_.use(IO.pure))

  private def doObserve(
      allShares: List[OwncloudShare]
  )(implicit
      ec: ContextShift[IO]
  ): Kleisli[IO, (Sardine, SharerConf), List[OwncloudShare]] =
    allShares
      .map(ocs =>
        OwncloudShares
          .listTopLevel(ocs.path)
          .mapF(
            _.handleErrorWith(
              {
                case x: SardineException if x.getStatusCode == NotFound.code => IO.pure(Nil)
                case x                                                       => IO.raiseError(x)
              }
            )
          )
          .map((ocs, _))
      )
      .parSequence
      .local[(Sardine, SharerConf)](dd => (dd._1, dd._2.owncloud.webdavBase))
      .tapWith((deps, results) => {
        results
          .collect {
            case (ocs, davResources) if davResources.hasFileNamed(deps._2.conditionsFileName) =>
              ocs
          }
      })

}
