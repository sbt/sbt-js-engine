addSbtPlugin("com.github.sbt" % "sbt-js-engine" % sys.props("project.version"))

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
)
