package nl.surf.rdx.librarian

import cats.effect.{ConcurrentEffect, IO, Timer}
import io.lemonlabs.uri.RelativeUrl
import nl.surf.rdx.librarian.conf.LibrarianConf
import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.circe.jsonOf

import scala.concurrent.ExecutionContext

object LibrarianRoutes {

  def getDatasetRoute(dsSearch: RelativeUrl => IO[Option[RdxDataset]]) =
    Router("/dataset" -> HttpRoutes.of[IO] {
      case GET -> Root / doi =>
        for {
          doi <- IO.fromTry(RelativeUrl.parseTry(doi))
          dsOption <- dsSearch(doi)
          response <- {
            dsOption match {
              case Some(ds) =>
                import RdxDataset.codecs._
                import org.http4s.circe.CirceEntityCodec._
                Ok(ds)
              case None => NotFound(s"Can not find dataset $doi")
            }
          }
        } yield response
    })

  def publishDatasetRoute(publish: RdxDataset => IO[Unit]) =
    Router("/dataset" -> HttpRoutes.of[IO] {
      case req @ POST -> Root =>
        import RdxDataset.codecs._
        import org.http4s.circe.CirceEntityCodec._
        implicit val decoder = jsonOf[IO, RdxDataset]

        for {
          ds <- req.as[RdxDataset]
          _ <- publish(ds)
        } yield Response(Status.Created)
    })

}
