lazy val `sbt-js-engine` = project in file(".")

enablePlugins(SbtWebBase)

lazy val scala212 = "2.12.20"
lazy val scala3 = "3.7.2"
ThisBuild / crossScalaVersions := Seq(scala212, scala3)

sonatypeProfileName := "com.github.sbt.sbt-js-engine" // See https://issues.sonatype.org/browse/OSSRH-77819#comment-1203625

description := "sbt js engine plugin"

developers += Developer(
  "playframework",
  "The Play Framework Team",
  "contact@playframework.com",
  url("https://github.com/playframework")
)

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "4.21.0" % "test",
  "org.specs2" %% "specs2-scalacheck" % "4.21.0" % "test",
  "io.spray" %% "spray-json" % "1.3.6",

  // Trireme
  "io.apigee.trireme" % "trireme-core" % "0.9.4",
  "io.apigee.trireme" % "trireme-node10src" % "0.9.4",

  // NPM
  "org.webjars" % "npm" % "5.0.0-2", // we are currently stuck: https://github.com/webjars/webjars/issues/1926
  "org.webjars" % "webjars-locator-core" % "0.59",

  // Test deps
  "junit" % "junit" % "4.13.2" % "test",

  // Cross build compatibility
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.13.0"

)

addSbtWeb("1.6.0-M1")

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

(pluginCrossBuild / sbtVersion) := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.10.2"
    case _      => "2.0.0-RC2"
  }
}

scalacOptions := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) => Seq("-Xsource:3")
    case _                => Seq.empty
  }
}
