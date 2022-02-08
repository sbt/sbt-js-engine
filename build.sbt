lazy val `sbt-js-engine` = project in file(".")

description := "sbt js engine plugin"

libraryDependencies ++= Seq(
  "com.typesafe" %% "jse" % "1.2.4",
  "com.typesafe" %% "npm" % "1.2.2",
  "org.specs2" %% "specs2-core" % "3.10.0" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.10.0" % "test",
  "junit" % "junit" % "4.12" % "test"
)

addSbtWeb("1.4.4")
