lazy val subproject = (project in file("./subproject")).enablePlugins(SbtWeb)

lazy val root = (project in file(".")).enablePlugins(SbtWeb)
  .settings(
    name := """sbt-web-sub-module-npm""",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.1"
  )

