lazy val `sbt-js-engine` = project in file(".")

description := "sbt js engine plugin"

libraryDependencies ++= Seq(
  "com.typesafe" %% "jse" % "1.2.4",
  "com.typesafe" %% "npm" % "1.2.1",
  "org.specs2" %% "specs2-core" % "4.14.0" % "test",
  "org.specs2" %% "specs2-scalacheck" % "4.14.0" % "test",
  "junit" % "junit" % "4.12" % "test"
)

addSbtWeb("1.4.4")
