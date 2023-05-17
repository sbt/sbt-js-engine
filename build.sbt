lazy val `sbt-js-engine` = project in file(".")

enablePlugins(SbtWebBase)

description := "sbt js engine plugin"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "4.10.3" % "test",
  "org.specs2" %% "specs2-scalacheck" % "4.10.3" % "test",
  "io.spray" %% "spray-json" % "1.3.6",

  // Trireme
  "io.apigee.trireme" % "trireme-core" % "0.9.4",
  "io.apigee.trireme" % "trireme-node10src" % "0.9.4",

  // NPM
  "org.webjars" % "npm" % "5.0.0-2",
  "org.webjars" % "webjars-locator-core" % "0.52",

  // Test deps
  "junit" % "junit" % "4.13.2" % "test"
)

addSbtWeb("1.5.0-M1")
