lazy val `sbt-js-engine` = project in file(".")

description := "sbt js engine plugin"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.10.0" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.10.0" % "test",
  "io.spray" %% "spray-json" % "1.3.3",

  // Trireme
  "io.apigee.trireme" % "trireme-core" % "0.8.9",
  "io.apigee.trireme" % "trireme-node10src" % "0.8.9",

  // NPM
  "org.webjars" % "npm" % "5.0.0-2",
  "org.webjars" % "webjars-locator-core" % "0.36",

  // Test deps
  "junit" % "junit" % "4.12" % "test"
)

addSbtWeb("1.4.4")

fork in Test := true
