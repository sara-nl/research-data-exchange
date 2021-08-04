package nl.surf.rdx.common.db

import cats.effect.{IO, IOApp, Sync}
import cats.implicits.{catsSyntaxFlatMapOps, toTraverseOps}
import nl.surf.rdx.common.db.conf.DbConf
import nl.surf.rdx.common.db.conf.DbConf.FlywayConf
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.jdk.CollectionConverters._

// TODO: integrate into app start or SBT
object MigrationApp extends IOApp.Simple {

  private implicit val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    for {
      config <- DbConf.load[IO]
      _ <- logger.info(
        "Running migrations from locations: " +
          config.flyway.migrationsLocations.mkString(", ")
      )
      plan <- migrationPlan(config)
      _ <- logValidationErrorsIfAny(plan)
      cnt <- IO(plan.load().migrate().migrationsExecuted)
      _ <- logger.info(s"Executed $cnt migrations")
    } yield ()

  private def migrationPlan(config: DbConf): IO[FluentConfiguration] =
    IO(
      Flyway.configure
        .dataSource(
          config.flyway.url,
          config.user,
          config.password.orNull
        )
        .group(true)
        .outOfOrder(false)
        .table(config.flyway.migrationsTable)
        .locations(
          config.flyway.migrationsLocations: _*
        )
        .baselineOnMigrate(true)
    )

  private def logValidationErrorsIfAny(m: FluentConfiguration): IO[Unit] =
    for {
      validated <- IO(
        m.ignorePendingMigrations(true)
          .load()
          .validateWithResult()
      )
      _ <- validated.invalidMigrations.asScala.toList.map(vo => logger.warn(s"""
                                                                      |Failed validation:
                                                                      |  - version: ${vo.version}
                                                                      |  - path: ${vo.filepath}
                                                                      |  - description: ${vo.description}
                                                                      |  - errorCode: ${vo.errorDetails.errorCode}
                                                                      |  - errorMessage: ${vo.errorDetails.errorMessage}
        """.stripMargin.strip)).sequence

    } yield ()

}
