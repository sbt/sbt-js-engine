addSbtPlugin("com.github.sbt" % "sbt-js-engine" % sys.props("project.version"))

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
)

libraryDependencies ++= Seq(
  "org.webjars" % "mkdirp" % "0.3.5"
)
