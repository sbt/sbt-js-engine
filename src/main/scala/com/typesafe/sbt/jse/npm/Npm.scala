package com.typesafe.sbt.jse.npm


import java.io.File

import com.typesafe.sbt.jse.engines.{Engine, JsExecutionResult, LocalEngine}

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

/**
  * A JVM class for performing NPM commands. Requires a JS engine to use.
  */
class Npm(engine: Engine, npmFile: Option[File] = None, preferSystemNpm: Boolean = true, verbose: Boolean = false) {

  @deprecated("Use one of the other non-deprecated constructors instead", "1.3.0")
  def this(engine: Engine, npmFile: File) = this(engine, Some(npmFile))

  @deprecated("Use one of the other non-deprecated constructors instead", "1.3.0")
  def this(engine: Engine, npmFile: File, verbose: Boolean) = this(engine, Some(npmFile), verbose)

  /**
    * https://docs.npmjs.com/cli/commands/npm-install
    */
  def install(global: Boolean = false, names: Seq[String] = Nil, outSink: String => Unit, errSink: String => Unit): JsExecutionResult = {
    cmd("install", global, names, outSink, errSink)
  }

  /**
    * https://docs.npmjs.com/cli/commands/npm-update
    */
  def update(global: Boolean = false, names: Seq[String] = Nil, outSink: String => Unit, errSink: String => Unit): JsExecutionResult = {
    cmd("update", global, names, outSink, errSink)
  }

  /**
    * https://docs.npmjs.com/cli/commands/npm-ci
    */
  def ci(names: Seq[String] = Nil, outSink: String => Unit, errSink: String => Unit): JsExecutionResult = {
    cmd("ci", false, names, outSink, errSink) // ci subcommand does not support -g (global) flag
  }

  private def cmd(cmd: String, global: Boolean = false, names: Seq[String] = Nil, outSink: String => Unit, errSink: String => Unit): JsExecutionResult = {
    val args = ListBuffer[String]()
    args += cmd
    if (global) args += "-g"
    if (verbose) args += "--verbose"
    args ++= names
    invokeNpm(args, outSink, errSink)
  }

  private def detectNpm(command: String): Option[String] = {
    val npmExists = Try(Process(s"$command --version").!!).isSuccess
    if (!npmExists) {
      println("!!!")
      println(s"Warning: npm detection failed. Tried the command: $command")
      println("!!!")
      None
    } else {
      Some(command)
    }
  }

  private def invokeNpm(args: ListBuffer[String], outSink: String => Unit, errSink: String => Unit): JsExecutionResult = {
    if (!engine.isNode) {
      throw new IllegalStateException("node not found: a Node.js installation is required to run npm.")
    }

    def executeJsNpm(): JsExecutionResult =
      engine.executeJs(npmFile.getOrElse(throw new RuntimeException("No NPM JavaScript file passed to the Npm instance via the npmFile param")), args.to(immutable.Seq), Map.empty, outSink, errSink)

    engine match {
      case localEngine: LocalEngine if preferSystemNpm =>
        // The first argument always is the command of the js engine, e.g. either just "node", "phantomjs,.. or a path like "/usr/bin/node"
        // So, if the command is a path, we first try to detect if there is a npm command available in the same folder
        val localEngineCmd = new File(localEngine.stdArgs.head)
        val localNpmCmd = if (localEngineCmd.getParent() == null) {
            // Pretty sure the command was not a path but just something like "node"
            // Therefore we assume the npm command is on the operating system path, just like the js engine command
            detectNpm("npm")
          } else {
            // Looks like the command was a valid path, so let's see if we can detect a npm command within the same folder
            // If we can't, try to fallback to a npm command that is on the operating system path
            val cmdPath = new File(localEngineCmd.getParentFile, "npm").getCanonicalPath
            detectNpm(cmdPath).orElse(detectNpm("npm"))
          }
        localNpmCmd match {
          case Some(cmd) =>
            val allArgs = immutable.Seq(cmd) ++ args
            val pb = new ProcessBuilder(LocalEngine.prepareArgs(allArgs).asJava)
            pb.environment().putAll(localEngine.stdEnvironment.asJava)
            JsExecutionResult(Process(pb).!(ProcessLogger(outSink, errSink)))
          case None =>
            println("!!!")
            println(s"Warning: npm detection failed. Falling back to npm provided by WebJars, which is outdated and will not work with Node versions 14 and newer.")
            println("!!!")
            executeJsNpm()
        }
      case _ => // e.g. Trireme provides node, but is not a local install and does not provide npm, therefore fallback using the webjar npm
        executeJsNpm()
    }
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
