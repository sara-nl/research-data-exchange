package nl.surf.rdx.common.db

import nl.surf.rdx.common.model.owncloud.OwncloudShare
import skunk.codec.all.{int2, varchar}
import cats.effect._
import cats.implicits._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.json._
import io.circe.syntax.EncoderOps
import nl.surf.rdx.common.model.{RdxDataset, RdxShare}
import io.circe.generic.auto._
import nl.surf.rdx.common.model.api.UserMetadata

import java.util.UUID

object Shares {

  def add: Command[RdxShare] = {
    val enc = (json ~ json ~ timestamptz ~ uuid.opt ~ timestamptz ~ varchar)
    sql"""INSERT INTO Shares (metadata, preview, created_at, token, token_expires_at, conditions_url) 
         VALUES ($enc)""".command.contramap {
      case RdxShare(share, createdAt, token, expiresAt, _, files, conditionsUrl) =>
        share.asJson ~ files.asJson ~ createdAt ~ token ~ expiresAt ~ conditionsUrl
    }
  }

  def delete(ss: List[OwncloudShare]): Command[ss.type] = {
    val enc = (varchar).values.contramap((s: OwncloudShare) => s.id).list(ss)
    sql"""DELETE FROM Shares 
         WHERE metadata->>'id' IN ($enc)""".command
  }

  def update: Command[(UserMetadata, UUID)] = {
    sql"""UPDATE Shares SET user_metadata = $json, published_at = NOW() WHERE token = $uuid AND token_expires_at >= NOW()""".command
      .contramap {
        case (um: UserMetadata, token: UUID) => um.asJson ~ token
      }
  }

  def invalidateAllExpired: Command[Void] = {
    sql"""UPDATE Shares SET token = NULL
         WHERE token_expires_at <= NOW()""".command
  }

  private val owncloudShareDec: Decoder[OwncloudShare] =
    (json).emap { case (p) => p.as[OwncloudShare].leftMap(_.getMessage()) }

  def listOC: Query[Void, OwncloudShare] = {
    sql"""SELECT metadata 
         FROM Shares""".query(owncloudShareDec)
  }

  def findShare: Query[UUID, RdxShare] = {
    val shareDec: Decoder[RdxShare] =
      (uuid ~ json ~ timestamptz ~ timestamptz ~ json ~ varchar).emap {
        case token ~ p ~ createdAt ~ expiresAt ~ files ~ conditionsUrl =>
          for {
            ocs <- p.as[OwncloudShare].leftMap(_.getMessage())
            fl <- files.as[List[String]].leftMap(_.getMessage())
          } yield RdxShare(
            ocs,
            createdAt,
            Some(token),
            expiresAt,
            ocs.additional_info_owner.orEmpty,
            fl,
            conditionsUrl
          )
      }

    sql"""SELECT token, metadata, created_at, token_expires_at, preview, conditions_url 
         FROM shares WHERE token = $uuid"""
      .query(shareDec)

  }

  def findDataset: Query[String, RdxDataset] = {
    val decoder = (text ~ text ~ varchar ~ json).emap {
      case title ~ description ~ conditionsUrl ~ list =>
        for {
          files <- list.as[List[String]].leftMap(_.getMessage())
        } yield RdxDataset(
          title,
          description,
          conditionsUrl,
          files
        )
    }

    sql"""SELECT user_metadata->>'title', user_metadata->>'description', conditions_url, preview 
         FROM shares WHERE user_metadata->>'doi' =  $varchar""".query(decoder)
  }

}
