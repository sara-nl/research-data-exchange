package nl.surf.rdx.common.db

import nl.surf.rdx.common.model.owncloud.OwncloudShare
import skunk.codec.all.{int2, varchar}
import cats.effect._
import cats.implicits._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.json._
import io.circe.Json
import io.circe.syntax.EncoderOps
import nl.surf.rdx.common.model.ShareToken
import scodec.codecs.{int, string}
import io.circe.generic.auto._

import java.time.{OffsetDateTime, ZonedDateTime}
import java.util.UUID

object Shares {

  def add: Command[ShareToken] = {
    val enc = (json ~ json ~ timestamptz ~ uuid ~ timestamptz)
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

  private val owncloudShareDec: Decoder[OwncloudShare] =
    (json).emap { case (p) => p.as[OwncloudShare].leftMap(_.getMessage()) }

  def list: Query[Void, OwncloudShare] = {
    sql"""Select metadata 
         FROM Shares""".query(owncloudShareDec)
  }

}
