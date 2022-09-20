package nl.surf.rdx.sharer

import cats.{Applicative, ApplicativeError, Functor, Monad, Parallel}
import cats.data.Kleisli
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Sync}
import natchez.Trace
import nl.surf.rdx.common.db.{DbSession, Shares}
import nl.surf.rdx.common.model.owncloud.OwncloudShare
import nl.surf.rdx.sharer.SharerApp.{Deps, EnvF}
import nl.surf.rdx.sharer.email.NewDatasetEml
import nl.surf.rdx.sharer.owncloud.OwncloudSharesObserver.Observation
import org.typelevel.log4cats.Logger
import cats.implicits._
import fs2.Pipe
import nl.surf.rdx.common.email.RdxEmail
import nl.surf.rdx.common.email.RdxEmail.Sealed
import nl.surf.rdx.common.model.RdxShare
object SharePipes {

  case class Result(
      removed: Set[OwncloudShare],
      added: Set[Observation]
  )

  private[sharer] def diff(
      stored: Set[OwncloudShare],
      observed: Set[Observation]
  ): Result = {
    val observedShares = observed.map(_.share)
    val removed = stored.foldLeft(Set.empty[OwncloudShare])((result, elem) =>
      if (observedShares.exists(_.id === elem.id)) result else result + elem
    )
    Result(
      removed,
      observed.filterNot(o => stored.exists(_.id.equals(o.share.id)))
    )
  }

  def onlyEligible[F[_]: Applicative: Functor: Logger: Parallel: Sync]
      : Pipe[EnvF[F, *], List[Observation], List[Observation]] =
    _.evalMap { observations =>
      for {
        deps <- Kleisli.ask[F, Deps[F]]
        res <- Kleisli.liftF[F, Deps[F], List[Observation]](
          observations.map(Sync[F].pure).parTraverseFilter {
            _ flatMap {
              case Observation(share, _) if !OwncloudShare.itemTypeFolder.equals(share.item_type) =>
                Logger[F].warn(
                  s"Share {id: ${share.id}} is ignored because it's not a folder"
                ) >> Sync[F].pure(none[Observation])
              case Observation(share, _)
                  if share.permissions < deps.ocConf.minimumPermissionLevel =>
                Logger[F].warn(
                  s"Share {id: ${share.id}} doesn't grant necessary re-sharing permissions"
                ) >> Sync[F].pure(none[Observation])
              case Observation(share, files)
                  if !files
                    .exists(_.endsWith(deps.conf.conditionsFileName)) =>
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

  def doMergeShares[F[_]: Applicative: Logger: Sync: ContextShift: ApplicativeError[
    *[_],
    Throwable
  ]: ConcurrentEffect: Trace: Parallel](
      observedDexShares: List[Observation]
  ): EnvF[F, List[RdxShare]] =
    Kleisli { deps =>
      DbSession
        .resource[F]
        .use(session => {
          for {
            storedShares <- session.execute(Shares.listOC)
            _ <- Logger[F].debug(
              s"Observed ${observedDexShares.size} shares, found ${storedShares.size} stored shares"
            )
            compared = SharePipes.diff(storedShares.toSet, observedDexShares.toSet)
            newShares <- (for {
                newShares <-
                  ShareEventHandlers
                    .handleShareAdded[F](compared.added.toList)
                    .local[Deps[F]](dd => ShareEventHandlers.Deps(session, dd.conf, dd.ocConf))
                _ <-
                  ShareEventHandlers
                    .handleShareRemoved[F](compared.removed.toList)
                    .local[Deps[F]](dd => ShareEventHandlers.Deps(session, dd.conf, dd.ocConf))
                _ <- Kleisli.liftF {
                  compared match {
                    case Result(removed, added) if removed.nonEmpty || added.nonEmpty =>
                      Logger[F]
                        .info(
                          s"Update finished. +${compared.added.size} shares / -${compared.removed.size} shares"
                        ) >>
                        Logger[F].info(
                          s"Added: (${compared.added.map(i => s"${i.share.id}:`${i.share.path}`").mkString(",")}), Deleted: (${compared.removed
                            .map(r => s"${r.id}:`${r.path}`")
                            .mkString(",")})"
                        )
                    case _ =>
                      Logger[F].debug(
                        s"Update finished. No changes found"
                      )
                  }

                }
              } yield newShares).run(deps)
          } yield newShares
        })
    }

  def notifyDataOwner[F[_]: Sync: Monad](newShares: List[RdxShare]): EnvF[F, Unit] =
    Kleisli { deps =>
      import RdxEmail.Template.syntax._
      deps.emailService
        .sendMany(
          newShares
            .map(NewDatasetEml.Vars[F](deps.conf.webUrl, _))
            .map(NewDatasetEml[F].seal)
        )
        .map(_ => ())
    }

}
