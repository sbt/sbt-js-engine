lazy val `sbt-js-engine` = project in file(".")

organization := "com.typesafe.sbt"
name := "sbt-js-engine"
description := "sbt js engine plugin"

scalaVersion := "2.10.4"
sbtPlugin := true

libraryDependencies ++= Seq(
  "com.typesafe" %% "jse" % "1.1.1",
  "com.typesafe" %% "npm" % "1.1.0",
  "org.specs2" %% "specs2-core" % "3.6" % "test",
  "junit" % "junit" % "4.12" % "test"
)
// Required by specs2 to get scalaz-stream
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.2.0")

scriptedSettings
scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

// Publish settings
publishMavenStyle := false
bintrayOrganization := Some("sbt-web")
bintrayRepository := "sbt-plugin-releases"
bintrayPackage := "sbt-js-engine"
bintrayReleaseOnPublish := false
homepage := Some(url("https://github.com/sbt/sbt-js-engine"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

// Release settings
releaseSettings
ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value
ReleaseKeys.tagName := (version in ThisBuild).value
ReleaseKeys.releaseProcess := {
  import sbtrelease._
  import ReleaseStateTransformations._

  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    ReleaseStep(action = releaseTask(bintrayRelease in `sbt-js-engine`)),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
}
