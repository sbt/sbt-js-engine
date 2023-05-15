package com.typesafe.sbt.jse.npm


import java.io.File

import com.typesafe.sbt.jse.engines.{Engine, JsExecutionResult}

import scala.collection.immutable
import scala.collection.mutable.ListBuffer

/**
  * A JVM class for performing NPM commands. Requires a JS engine to use.
  */
class Npm(engine: Engine, npmFile: File, verbose: Boolean = false) {

  def this(engine: Engine, npmFile: File) = this(engine, npmFile, false)

  def update(global: Boolean = false, names: Seq[String] = Nil, outSink: String => Unit, errSink: String => Unit): JsExecutionResult = {
    val args = ListBuffer[String]()
    args += "update"
    if (global) args += "-g"
    if (verbose) args += "--verbose"
    args ++= names
    invokeNpm(args, outSink, errSink)
  }

  private def invokeNpm(args: ListBuffer[String], outSink: String => Unit, errSink: String => Unit): JsExecutionResult = {
    if (!engine.isNode) {
      throw new IllegalStateException("node not found: a Node.js installation is required to run npm.")
    }
    engine.executeJs(npmFile, args.to[immutable.Seq], Map.empty, outSink, errSink)
  }

}


import org.webjars.WebJarExtractor

object NpmLoader {
  /**
    * Extract the NPM WebJar to disk and return its main entry point.
    * @param to The directory to extract to.
    * @param classLoader The classloader that should be used to locate the Node related WebJars.
    * @return The main JavaScript entry point into NPM.
    */
  def load(to: File, classLoader: ClassLoader): File = {
    val extractor = new WebJarExtractor(classLoader)
    extractor.extractAllNodeModulesTo(to)
    new File(to, "npm" + File.separator + "lib" + File.separator + "npm.js")
  }
}
