package com.typesafe.sbt.jse.engines

import java.io._
import java.util.concurrent.{ForkJoinPool, TimeUnit}

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import io.apigee.trireme.core._
import org.mozilla.javascript.RhinoException

import scala.concurrent.ExecutionException
import scala.util.control.NonFatal

class Trireme(
  stdArgs: immutable.Seq[String],
  stdEnvironment: Map[String, String]
) extends Engine {

  private val AwaitTerminationTimeout = 1.second

  override def executeJs(source: File, args: immutable.Seq[String], environment: Map[String, String],
    stdOutSink: String => Unit, stdErrSink: String => Unit): JsExecutionResult = {

    val file = source.getCanonicalFile

    val env = (sys.env ++ stdEnvironment ++ environment).asJava
    val sandbox = new Sandbox()
    sandbox.setAsyncThreadPool(ForkJoinPool.commonPool())
    val nodeEnv = new NodeEnvironment()
    nodeEnv.setSandbox(sandbox)
    sandbox.setStdin(new ByteArrayInputStream(Array()))
    sandbox.setStdout(new LineSinkOutputStream(stdOutSink))
    val stderrOs = new LineSinkOutputStream(stdErrSink)
    sandbox.setStderr(stderrOs)

    val script = nodeEnv.createScript(file.getName, file, (stdArgs ++ args).toArray)
    script.setEnvironment(env)

    try {
      val status = script.execute.get()
      if (status.hasCause) {
        handleError(status.getCause, stderrOs)
      } else {
        JsExecutionResult(0)
      }
    } catch {
      case NonFatal(e) => handleError(e, stderrOs)
    } finally {
      script.close()
      nodeEnv.getScriptPool.shutdown()
      nodeEnv.getScriptPool.awaitTermination(AwaitTerminationTimeout.toMillis, TimeUnit.MILLISECONDS)
    }
  }

  private def handleError(error: Throwable, stderrOs: OutputStream): JsExecutionResult = {
    error match {
      case ee: ExecutionException =>
        handleError(ee.getCause, stderrOs)
      case e: RhinoException =>
        stderrOs.write(e.getLocalizedMessage.getBytes("UTF-8"))
        stderrOs.write(e.getScriptStackTrace.getBytes("UTF-8"))
        JsExecutionResult(1)
      case t =>
        t.printStackTrace(new PrintStream(stderrOs))
        JsExecutionResult(1)
    }
  }

  override def isNode: Boolean = true
}

object Trireme {
  def apply(
    stdArgs: immutable.Seq[String] = Nil,
    stdEnvironment: Map[String, String] = Map.empty
  ): Trireme = new Trireme(stdArgs, stdEnvironment)
}