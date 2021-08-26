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
import nl.surf.rdx.common.model.{ShareToken, UserMetadata}
import io.circe.generic.auto._

import java.util.UUID

object Shares {

  def add: Command[ShareToken] = {
    val enc = (json ~ json ~ timestamptz ~ uuid.opt ~ timestamptz)
    sql"""INSERT INTO Shares (metadata, preview, created_at, token, token_expires_at) 
         VALUES ($enc)""".command.contramap {
      case ShareToken(share, createdAt, token, expiresAt, _, files) =>
        share.asJson ~ files.asJson ~ createdAt ~ token ~ expiresAt
    }
  }

  def delete(ss: List[OwncloudShare]): Command[ss.type] = {
    val enc = (varchar).values.contramap((s: OwncloudShare) => s.id).list(ss)
    sql"""DELETE FROM Shares 
         WHERE metadata->>'id' IN ${enc}""".command
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

  def list: Query[Void, OwncloudShare] = {
    sql"""SELECT metadata 
         FROM Shares""".query(owncloudShareDec)
  }

  def find(token: UUID): Query[UUID, ShareToken] = {
    val shareDec: Decoder[ShareToken] =
      (json ~ timestamptz ~ timestamptz ~ json).emap {
        case p ~ createdAt ~ expiresAt ~ files =>
          for {
            ocs <- p.as[OwncloudShare].leftMap(_.getMessage())
            fl <- files.as[List[String]].leftMap(_.getMessage())
          } yield ShareToken(
            ocs,
            createdAt,
            Some(token),
            expiresAt,
            ocs.additional_info_owner.orEmpty,
            fl
          )
      }

    sql"""SELECT metadata, created_at, token_expires_at, preview FROM shares where token = $uuid"""
      .query(shareDec)

  }

}
