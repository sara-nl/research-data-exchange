ThisBuild / name := "hello-world"

ThisBuild / idePackagePrefix := Some("nl.surf.rdx")

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.13.6"

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
)

ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps"
)

lazy val backend = (project in file("."))
  .settings(
    name := "backend",
    libraryDependencies := Seq(deps.catsEffect, deps.pencil) ++ deps.logging
  )

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

  val pencil = "com.minosiants" %% "pencil" % "0.6.7"

  val http4sClient =
    Seq(
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-blaze-client" % V.http4s,
      "org.http4s" %% "http4s-circe" % V.http4s
    )
  val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
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

}
