package nl.surf.rdx.sharer

import cats.{Applicative, Monad, Parallel}
import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift, Sync}
import nl.surf.rdx.common.model.RdxShare
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.Observation
import skunk.Session
import cats.implicits._
import nl.surf.rdx.common.db.Shares
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.common.owncloud.OwncloudShares
import nl.surf.rdx.common.owncloud.conf.OwncloudConf
import nl.surf.rdx.sharer.conf.SharerConf
import org.typelevel.log4cats.Logger

import java.nio.file.Paths

object ShareEventHandlers {

  case class Deps[F[_]](session: Session[F], conf: SharerConf, owncloudConf: OwncloudConf)

  def handleShareAdded[F[
      _
  ]: Parallel: Sync: Monad: Logger: ConcurrentEffect: ContextShift: Applicative](
      observations: List[Observation]
  ): Kleisli[F, Deps[F], List[RdxShare]] =
    Kleisli {
      case Deps(session, conf, ocConf) =>
        for {
          newTokens <-
            observations
              .parTraverseFilter {
                case Observation(share, files) =>
                  for {
                    conditionsPath <- Sync[F].fromOption(
                      files.find(
                        _.toString.toLowerCase() === conf.conditionsFileName.toLowerCase()
                      ),
                      new RuntimeException(
                        s"Can not find conditions document in share ${share.path}"
                      )
                    )
                    mkPublicLink <- OwncloudShares.makePublicLink[F].run(ocConf)
                    conditionsUrl <- mkPublicLink(
                      Paths.get(share.path).resolve(conditionsPath)
                    )
                    res <-
                      RdxShare
                        .createFor[F](share, files, conf.tokenValidityInterval, conditionsUrl)
                  } yield res
              }
          _ <-
            session
              .prepare(Shares.add)
              .use(prep => newTokens.map(prep.execute).sequence)
          _ <- Logger[F].debug("Finished handling added shares")
        } yield newTokens

    }

  def handleShareRemoved[F[_]: Parallel: Sync: Monad: Logger](
      shares: List[OwncloudShare]
  ): Kleisli[F, Deps[F], Unit] =
    Kleisli {
      case Deps(session, _, _) =>
        if (shares.nonEmpty)
          session
            .prepare(Shares.delete(shares))
            .use(_.execute(shares)) >> Sync[F].unit
        else
          Sync[F].unit
    }

}
