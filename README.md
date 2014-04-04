sbt-js-engine
=============

[![Build Status](https://api.travis-ci.org/sbt/sbt-js-engine.png?branch=master)](https://travis-ci.org/sbt/sbt-js-engine)

This plugin mainly provides support for the authoring of sbt plugins that require js-engine.

Of particular note is SbtJsTaskPlugin. This plugin provides an abstract base intended to be used for creating
the various types of plugin for sbt-web. At this time the types of plugin are "source file plugins" and "other".

Source file plugins use the `sources` and `sourceDirectories` sbt keys and process files from there. Plugins of this
type can optionally produce managed resources.

The other types of plugin are ones that wish to just invoke the js engine and so there are helper functions to do
that.

sbt-js-engine also enhances sbt-web with [npm](https://www.npmjs.org/) functionality. If a `package.json` file
is found in the project's base directory then it will cause npm to run. By default npm will run in the JVM but just
as with other sbt-js-engine plugins, the type of engine can be configured. For example to use Node directly:

```scala
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
```

Alternatively you can provide a system property via SBT_OPTS, for example:

```bash
export SBT_OPTS="$SBT_OPTS -Dsbt.jse.engineType=Node"
```

npm extracts its artifacts into the node_modules folder of a base directory and makes the contents available to
sbt-web plugins as a whole. Note that sbt-js-engines loads the
actual source code of npm via a WebJar and invokes an "npm update". Any external npm activity can therefore be performed
interchangeably with sbt-js-engine in place.

&copy; Typesafe Inc., 2014