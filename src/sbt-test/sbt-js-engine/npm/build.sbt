lazy val root = (project in file(".")).enablePlugins(SbtWeb)

ThisBuild / scalaVersion := {
  if (sbtVersion.value.startsWith("1.")) "2.12.21" else "3.9.0"
}
