package com.typesafe.sbt.jse.engines

import java.io.File

import scala.jdk.CollectionConverters.*
import scala.collection.immutable
import scala.sys.process.{Process, ProcessLogger}

class LocalEngine(val stdArgs: immutable.Seq[String], val stdEnvironment: Map[String, String], override val isNode: Boolean) extends Engine {

  override def executeJs(source: File, args: immutable.Seq[String], environment: Map[String, String],
    stdOutSink: String => Unit, stdErrSink: String => Unit): JsExecutionResult = {

    val allArgs = (stdArgs :+ source.getCanonicalPath) ++ args
    val allEnvironment = stdEnvironment ++ environment


    val pb = new ProcessBuilder(LocalEngine.prepareArgs(allArgs).asJava)
    pb.environment().putAll(allEnvironment.asJava)
    JsExecutionResult(Process(pb).!(ProcessLogger(stdOutSink, stdErrSink)))
  }
}


/**
  * Local engine utilities.
  */
object LocalEngine {

  def path(path: Option[File], command: String): String = path.fold(command)(_.getCanonicalPath)

  val nodePathDelim: String = if (System.getProperty("os.name").toLowerCase.contains("win")) ";" else ":"

  def nodePathEnv(modulePaths: immutable.Seq[String]): Map[String, String] = {
    val nodePath = modulePaths.mkString(nodePathDelim)
    val newNodePath = Option(System.getenv("NODE_PATH")).fold(nodePath)(_ + nodePathDelim + nodePath)
    if (newNodePath.isEmpty) Map.empty[String, String] else Map("NODE_PATH" -> newNodePath)
  }

  // This quoting functionality is as recommended per http://bugs.java.com/view_bug.do?bug_id=6511002
  // The JDK can't change due to its backward compatibility requirements, but we have no such constraint
  // here. Args should be able to be expressed consistently by the user of our API no matter whether
  // execution is on Windows or not.
  private def needsQuoting(s: String): Boolean =
    if (s.isEmpty) true else s.exists(c => c == ' ' || c == '\t' || c == '\\' || c == '"')

  private def winQuote(s: String): String = {
    if (!needsQuoting(s)) {
      s
    } else {
      "\"" + s.replaceAll("([\\\\]*)\"", "$1$1\\\\\"").replaceAll("([\\\\]*)\\z", "$1$1") + "\""
    }
  }

  private val isWindows: Boolean = System.getProperty("os.name").toLowerCase.contains("win")

  private[jse] def prepareArgs(args: immutable.Seq[String]): immutable.Seq[String] =
    if (isWindows) args.map(winQuote) else args
}

/**
  * Used to manage a local instance of Node.js with CommonJs support. common-node is assumed to be on the path.
  */
object CommonNode {

  import LocalEngine._

  def apply(command: Option[File] = None, stdArgs: immutable.Seq[String] = Nil, stdEnvironment: Map[String, String] = Map.empty): LocalEngine = {
    val args = immutable.Seq(path(command, "common-node")) ++ stdArgs
    new LocalEngine(args, stdEnvironment, true)
  }
}

/**
  * Used to manage a local instance of Node.js. Node is assumed to be on the path.
  */
object Node {

  import LocalEngine._

  def apply(command: Option[File] = None, stdArgs: immutable.Seq[String] = Nil, stdEnvironment: Map[String, String] = Map.empty): LocalEngine = {
    val args = immutable.Seq(path(command, "node")) ++ stdArgs
    new LocalEngine(args, stdEnvironment, true)
  }
}

/**
  * Used to manage a local instance of PhantomJS. PhantomJS is assumed to be on the path.
  */
object PhantomJs {

  import LocalEngine._

  def apply(command: Option[File] = None, stdArgs: immutable.Seq[String] = Nil, stdEnvironment: Map[String, String] = Map.empty): LocalEngine = {
    val args = immutable.Seq(path(command, "phantomjs")) ++ stdArgs
    new LocalEngine(args, stdEnvironment, false)
  }
}
