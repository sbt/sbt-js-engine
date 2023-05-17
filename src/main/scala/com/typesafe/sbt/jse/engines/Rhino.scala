package com.typesafe.sbt.jse.engines


import java.io._
import java.net.URI

import scala.collection.immutable
import org.mozilla.javascript._
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.{SoftCachingModuleScriptProvider, UrlModuleSourceProvider}
import org.mozilla.javascript.tools.shell.Global

class Rhino(
  stdArgs: immutable.Seq[String],
  stdModulePaths: immutable.Seq[String]
) extends Engine {

  override def executeJs(source: File, args: immutable.Seq[String], environment: Map[String, String],
    stdOutSink: String => Unit, stdErrSink: String => Unit): JsExecutionResult = {

    val script = source.getCanonicalFile

    val requireBuilder = {
      import scala.collection.JavaConverters._
      val paths = script.getParentFile.toURI +: stdModulePaths.map(new URI(_))
      val sourceProvider = new UrlModuleSourceProvider(paths.asJava, null)
      val scriptProvider = new SoftCachingModuleScriptProvider(sourceProvider)
      new RequireBuilder().setModuleScriptProvider(scriptProvider)
    }

    val ctx = Context.enter()
    val stderrOs = new LineSinkOutputStream(stdErrSink)

    try {

      // Create a global object so that we have Rhino shell functions in scope (e.g. load, print, ...)
      val global = {
        val g = new Global()
        g.init(ctx)
        g.setIn(new ByteArrayInputStream(Array()))
        g.setOut(new PrintStream(new LineSinkOutputStream(stdOutSink)))
        g.setErr(new PrintStream(stderrOs))
        g
      }

      // Prepare a scope by passing the arguments and adding CommonJS support
      val scope = {
        val s = ctx.initStandardObjects(global, false)
        s.defineProperty("arguments", (stdArgs ++ args).toArray, ScriptableObject.READONLY)
        val require = requireBuilder.createRequire(ctx, s)
        require.install(s)
        s
      }

      // Evaluate
      val reader = new FileReader(script)
      ctx.evaluateReader(scope, reader, script.getName, 0, null)
      JsExecutionResult(0)

    } catch {

      case e: RhinoException =>
        stderrOs.write(e.getLocalizedMessage.getBytes("UTF-8"))
        stderrOs.write(e.getScriptStackTrace.getBytes("UTF-8"))
        JsExecutionResult(1)

      case t: Exception =>
        t.printStackTrace(new PrintStream(stderrOs))
        JsExecutionResult(1)

    } finally {
      Context.exit()
    }

  }

  override def isNode: Boolean = false
}

object Rhino {
  def apply(
    stdArgs: immutable.Seq[String] = Nil,
    stdModulePaths: immutable.Seq[String] = Nil
  ): Rhino = new Rhino(stdArgs, stdModulePaths)
}