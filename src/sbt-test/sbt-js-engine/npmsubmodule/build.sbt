lazy val subproject = (project in file("./subproject")).enablePlugins(SbtWeb)

ThisBuild / scalaVersion := {
  if (sbtVersion.value.startsWith("1.")) "2.12.21" else "3.9.0"
}

lazy val root = (project in file(".")).enablePlugins(SbtWeb)
  .settings(
    name := """sbt-web-sub-module-npm""",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.1"
  )
