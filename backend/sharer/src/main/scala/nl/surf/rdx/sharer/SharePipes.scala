package nl.surf.rdx.sharer

import cats.{Applicative, Functor, Parallel}
import cats.data.Kleisli
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Sync}
import natchez.Trace
import nl.surf.rdx.common.db.{DbSession, Shares}
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.SharerApp.{Deps, EnvF}
import nl.surf.rdx.sharer.email.{RdxEmail, RdxNotification}
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.{Observation, Observation2}
import org.typelevel.log4cats.Logger
import cats.implicits._
import fs2.Pipe

object SharePipes {

  case class Result(
      removed: Set[OwncloudShare],
      added: Set[Observation]
  )

  private[sharer] def diff(
      stored: Set[OwncloudShare],
      observed: Set[Observation]
  ): Result = {
    Result(stored.diff(observed.map(_.share)), observed.filterNot(o => stored.contains(o.share)))
  }

  def onlyElegible[F[_]: Applicative: Functor: Logger: Parallel: Sync]
      : Pipe[EnvF[F, *], List[Observation], List[Observation]] =
    _.evalMap { observations =>
      for {
        deps <- Kleisli.ask[F, SharerApp.Deps]
        res <- Kleisli.liftF[F, SharerApp.Deps, List[Observation]](
          observations.map(Sync[F].pure).parTraverseFilter {
            _ flatMap {
              case Observation(share, _) if !OwncloudShare.itemTypeFolder.equals(share.item_type) =>
                Logger[F].warn(
                  s"Share {id: ${share.id}} is ignored because it's not a folder"
                ) >> Sync[F].pure(none[Observation])
              case Observation(share, _)
                  if share.permissions < deps.conf.owncloud.minimumPermissionLevel =>
                Logger[F].warn(
                  s"Share {id: ${share.id}} doesn't grant necessary re-sharing permissions"
                ) >> Sync[F].pure(none[Observation])
              case Observation(share, files)
                  if !files
                    .contains(
                      deps.conf.conditionsFileName.toLowerCase
                    ) =>
                Logger[F].warn(
                  s"Share {id: ${share.id}} is ignored because it doesn't have conditions file"
                ) >> Sync[F].pure(none[Observation])
              case o @ Observation(share, _) =>
                Logger[F].debug(s"Share {id: ${share.id}} passes RDX criteria") >> Sync[F]
                  .pure(o.some)
            }
          }
        )
      } yield res
    }

  //TODO: split this into small nice testable pipes
  def doMergeShares[F[
      _
  ]: Applicative: Logger: Sync: ContextShift: Concurrent: ConcurrentEffect: Trace: Parallel](
      observedDexShares: List[Observation]
  ): EnvF[F, Unit] =
    Kleisli { deps =>
      DbSession
        .resource[F]
        .use(session => {
          for {
            storedShares <- session.execute(Shares.listOC)
            _ <- Logger[F].info(
              s"Observed ${observedDexShares.size} shares, found ${storedShares.size} stored shares"
            )
            compared = SharePipes.diff(storedShares.toSet, observedDexShares.toSet)
            _ <- (for {
                newShareTokens <-
                  ShareEventHandlers
                    .handleShareAdded[F](compared.added.toList)
                    .local[Deps](dd => ShareEventHandlers.Deps(session, dd.conf))
                _ <-
                  ShareEventHandlers
                    .handleShareRemoved[F](compared.removed.toList)
                    .local[Deps](dd => ShareEventHandlers.Deps(session, dd.conf))
                _ <- Kleisli.liftF(
                  Logger[F].info(
                    s"Update finished. +${compared.added.size} shares / -${compared.removed.size} shares"
                  )
                )
                emailMessages <-
                  newShareTokens
                    .map(RdxNotification.newToken[F])
                    .parSequence
                    .local[Deps](_.conf)
                _ <-
                  emailMessages
                    .map(
                      RdxEmail
                        .send(_)
                        .local[Deps](_.conf.email)
                        .flatTap(_ => Kleisli.liftF(Logger[F].debug("Emails sent")))
                    )
                    .parSequence

              } yield ()).run(deps)
          } yield ()
        })
    }

}
