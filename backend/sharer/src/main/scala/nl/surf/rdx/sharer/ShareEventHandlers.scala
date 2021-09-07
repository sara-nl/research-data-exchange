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
import nl.surf.rdx.sharer.conf.SharerConf
import nl.surf.rdx.sharer.owncloud.OwncloudShares
import org.typelevel.log4cats.Logger

object ShareEventHandlers {

  case class Deps[F[_]](session: Session[F], conf: SharerConf)

  def handleShareAdded[F[
      _
  ]: Parallel: Sync: Monad: Logger: ConcurrentEffect: ContextShift: Applicative](
      observations: List[Observation]
  ): Kleisli[F, Deps[F], List[RdxShare]] =
    for {
      shareTokens <- Kleisli((deps: Deps[F]) =>
        for {
          Deps(session, conf) <- Sync[F].pure(deps)
          newTokens <-
            observations
              .parTraverseFilter {
                case Observation(share, files) =>
                  import io.lemonlabs.uri.typesafe.dsl.pathPartToUrlDsl
                  for {
                    conditionsFile <- Sync[F].fromOption(
                      files.find(_.toLowerCase() === conf.conditionsFileName.toLowerCase()),
                      new RuntimeException(
                        s"Can not find conditions document in share ${share.path}"
                      )
                    )
                    conditionsUrl <-
                      OwncloudShares
                        .makePublicLink[F](
                          (share.path / conditionsFile)
                            .normalize(removeEmptyPathParts = true)
                            .toStringPunycode
                        )
                        .run(OwncloudShares.Deps(conf.owncloud, conf.client))
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
      )
    } yield shareTokens

  def handleShareRemoved[F[_]: Parallel: Sync: Monad: Logger](
      shares: List[OwncloudShare]
  ): Kleisli[F, Deps[F], Unit] =
    Kleisli {
      case Deps(session, _) =>
        if (shares.nonEmpty)
          session
            .prepare(Shares.delete(shares))
            .use(_.execute(shares)) >> Sync[F].unit
        else
          Sync[F].unit
    }

}
