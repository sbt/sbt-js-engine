lazy val `sbt-js-engine` = project in file(".")

description := "sbt js engine plugin"

libraryDependencies ++= Seq(
  "com.typesafe" %% "jse" % "1.2.3",
  "com.typesafe" %% "npm" % "1.2.1",
  "org.specs2" %% "specs2-core" % "3.8.9" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.8.9" % "test",
  "junit" % "junit" % "4.12" % "test"
)

addSbtWeb("1.4.2")
