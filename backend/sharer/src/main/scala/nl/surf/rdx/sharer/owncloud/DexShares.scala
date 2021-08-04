package nl.surf.rdx.sharer.owncloud

import cats.Functor
import cats.data.Kleisli
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits._
import com.github.sardine.Sardine
import com.github.sardine.impl.SardineException
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.SharerApp
import nl.surf.rdx.sharer.SharerApp.EnvF
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.webdav.Webdav
import nl.surf.rdx.sharer.owncloud.webdav.Webdav.implicits._
import org.http4s.Status.NotFound
import org.typelevel.log4cats.Logger

object DexShares {

  case class Observation(share: OwncloudShare, files: List[String])

  def stream(implicit
      cs: ContextShift[IO],
      timer: Timer[IO],
      logger: Logger[EnvF[IO, *]]
  ): EnvF[fs2.Stream[EnvF[IO, *], *], List[Observation]] =
    Kleisli { deps: SharerApp.Deps =>
      fs2.Stream
        .awakeEvery[EnvF[IO, *]](deps.conf.fetchInterval)
        .evalTap(_ => logger.debug(s"Starting shares fetch"))
        .evalMap(_ => DexShares.observe)
    }

  /**
    * Fetches recent DEX shares (i.e. shares that are folders and contain conditions document).
    */
  def observe(implicit
      ec: ContextShift[IO]
  ): Kleisli[IO, SharerApp.Deps, List[Observation]] =
    (for {
      allShares <-
        OwncloudShares.getShares
          .mapF[Resource[IO, *], List[OwncloudShare]](Resource.eval)
          .local[SharerApp.Deps](_.sharesDeps)
      sardineR <- Webdav.makeSardine.local[SharerApp.Deps](_.conf.owncloud)
      dexShares <- doObserve(allShares)
        .mapF[Resource[IO, *], List[Observation]](Resource.eval)
        .local[SharerApp.Deps] { case SharerApp.Deps(_, conf) => (sardineR, conf) }
    } yield dexShares).mapF(_.use(IO.pure))

  private def doObserve(
      allShares: List[OwncloudShare]
  )(implicit
      ec: ContextShift[IO]
  ): Kleisli[IO, (Sardine, SharerConf), List[Observation]] =
    allShares
      .filter(_.item_type === OwncloudShare.itemTypeFolder)
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
              Observation(ocs, davResources.map(_.getPath))
          }
      })

}
