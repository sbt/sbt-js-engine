addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % sys.props("project.version"))

resolvers ++= Seq(
  Resolver.url("sbt snapshot plugins", url("https://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "https://repo.typesafe.com/typesafe/snapshots/"
)

libraryDependencies ++= Seq(
  "org.webjars" % "mkdirp" % "0.3.5"
)
