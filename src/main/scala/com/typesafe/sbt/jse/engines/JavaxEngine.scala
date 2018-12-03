package com.typesafe.sbt.jse.engines


import java.io._

import javax.script._

import scala.collection.immutable

class JavaxEngine(
  stdArgs: immutable.Seq[String],
  engineName: String
) extends Engine {

  private val engine = new ScriptEngineManager(null).getEngineByName(engineName)

  if (engine == null) throw new Exception(s"Javascript engine '$engineName' not found")

  override def executeJs(source: File, args: immutable.Seq[String], environment: Map[String, String],
    stdOutSink: String => Unit, stdErrSink: String => Unit): JsExecutionResult = {

    val script = source.getCanonicalFile

    val scriptReader = new FileReader(script)
    val stdErrWriter = new LineSinkWriter(stdErrSink)

    val context = {
      val c: ScriptContext = new SimpleScriptContext()
      c.setReader(new StringReader(""))
      c.setWriter(new LineSinkWriter(stdOutSink))
      c.setErrorWriter(stdErrWriter)
      // If you create a new ScriptContext object and use it to evaluate scripts, then
      // ENGINE_SCOPE of that context has to be associated with a nashorn Global object somehow.
      // See https://wiki.openjdk.java.net/display/Nashorn/Nashorn+jsr223+engine+notes
      c.setBindings(engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE), ScriptContext.ENGINE_SCOPE)
      c.setAttribute("arguments", (stdArgs ++ args).toArray, ScriptContext.ENGINE_SCOPE)
      c.setAttribute(ScriptEngine.FILENAME, script.getName, ScriptContext.ENGINE_SCOPE)
      c
    }

    try {
      engine.eval(scriptReader, context)
      JsExecutionResult(0)
    } catch {
      case e: ScriptException =>
        e.printStackTrace(new PrintWriter(stdErrWriter))
        JsExecutionResult(1)
    }
  }

  override def isNode: Boolean = false
}

object JavaxEngine {

  def apply(
    stdArgs: immutable.Seq[String] = Nil,
    engineName: String = "js"
  ): JavaxEngine = new JavaxEngine(stdArgs, engineName)

}