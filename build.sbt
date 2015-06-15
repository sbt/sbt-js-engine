lazy val `sbt-js-engine` = project in file(".")

description := "sbt js engine plugin"

libraryDependencies ++= Seq(
  "com.typesafe" %% "jse" % "1.1.2",
  "com.typesafe" %% "npm" % "1.1.0",
  "org.specs2" %% "specs2-core" % "3.6" % "test",
  "junit" % "junit" % "4.12" % "test"
)
// Required by specs2 to get scalaz-stream
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

addSbtWeb("1.2.0")

