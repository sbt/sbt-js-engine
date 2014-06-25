sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-js-engine"

version := "1.0.2-SNAPSHOT"

scalaVersion := "2.10.4"

resolvers ++= Seq(
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe" %% "jse" % "1.0.0",
  "com.typesafe" %% "npm" % "1.0.0",
  "org.specs2" %% "specs2" % "2.3.11" % "test",
  "junit" % "junit" % "4.11" % "test"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.0.2")

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

publishMavenStyle := false

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}
