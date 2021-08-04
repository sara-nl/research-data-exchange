ThisBuild / organization := "nl.surf"

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.13.6"

ThisBuild / useCoursier := false

ThisBuild / assemblyMergeStrategy := {
  case "META-INF/services/org.flywaydb.core.internal.database.DatabaseType" =>
    MergeStrategy.first
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "application.conf"            => MergeStrategy.concat
  case "reference.conf"              => MergeStrategy.concat
  case x                             => MergeStrategy.first
}

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps"
)

val commonSettings = Seq(
//  TODO: figure out how to make this work...
)

lazy val backend = (project in file("."))
  .settings(
    name := "backend"
  )
  .dependsOn(sharer, librarian)

lazy val sharer = (project in file("sharer"))
  .settings(
    libraryDependencies := Seq(
      deps.scalaTest,
      deps.betterFiles,
      deps.catsEffect,
      deps.scalaUri,
      deps.pencil
    ) ++ deps.pureConfig ++ deps.sardine ++ deps.circe ++ deps.http4sClient ++ deps.logging
  )
  .dependsOn(commonDb)

lazy val librarian = (project in file("librarian"))
  .settings(
    libraryDependencies := Seq(
      deps.scalaTest,
      deps.betterFiles,
      deps.catsEffect,
      deps.scalaUri,
      deps.scalaTest,
      deps.catsTestkit
    ) ++ deps.pureConfig ++ deps.circe ++ deps.http4sServer ++ deps.logging
  )
  .dependsOn(commonModel)

lazy val commonModel = (project in file("common/model")).settings(
  libraryDependencies := Seq(
    deps.catsEffect
  ) ++ deps.logging
)

lazy val commonDb = (project in file("common/db"))
  .settings(
    libraryDependencies := Seq(
      deps.catsEffect
    ) ++ deps.skunk ++ deps.flyway
      ++ deps.logging ++ deps.pureConfig ++ deps.circe
  )
  .dependsOn(commonModel)

lazy val deps = new {

  lazy val V = new {
    val http4s = "0.21.24"
    val catsEffect = "2.5.1"
    val pureConf = "0.16.0"
    val logback = "1.2.3"
    val log4cats = "1.3.1"
    val circe = "0.14.1"
    val artc = "0.1.3"
  }

  val http4sServer =
    Seq(
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-blaze-server" % V.http4s,
      "org.http4s" %% "http4s-circe" % V.http4s
    )
  val http4sClient =
    Seq(
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-blaze-client" % V.http4s,
      "org.http4s" %% "http4s-circe" % V.http4s
    )
  val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
  val catsTestkit = "org.typelevel" %% "cats-testkit-scalatest" % "2.1.5" % Test
  val betterFiles = "com.github.pathikrit" %% "better-files" % "3.9.1"
  val scalaUri = "io.lemonlabs" %% "scala-uri" % "3.5.0"

  val artc = "io.github.mkotsur" %% "artc" % V.artc
//  val tapir = "com.softwaremill.sttp.tapir" %% "tapir-core" % V.tapir
//  val tapirHttps = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapir
  val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig" % V.pureConf,
    "com.github.pureconfig" %% "pureconfig-cats-effect2" % V.pureConf
  )

  val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % V.circe)

  val logging = Seq(
    "ch.qos.logback" % "logback-core" % V.logback,
    "ch.qos.logback" % "logback-classic" % V.logback,
    "org.typelevel" %% "log4cats-slf4j" % V.log4cats
  )

  val sardine = Seq(
    "com.github.lookfirst" % "sardine" % "5.9",
    "javax.xml.bind" % "jaxb-api" % "2.4.0-b180830.0359",
    "javax.activation" % "activation" % "1.1.1",
    "org.glassfish.jaxb" % "jaxb-runtime" % "2.4.0-b180830.0438"
  )

  val pencil = "com.minosiants" %% "pencil" % "0.6.7"

  val skunk =
    Seq("org.tpolecat" %% "skunk-core" % "0.0.28", "org.tpolecat" %% "skunk-circe" % "0.0.28")
  val flyway =
    Seq("org.postgresql" % "postgresql" % "42.2.23", "org.flywaydb" % "flyway-core" % "7.11.4")

}