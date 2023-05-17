package com.typesafe.sbt.jse.engines

import java.io.File

import scala.collection.immutable

/**
  * A JavaScript engine. JavaScript engines are intended to be short-lived and will terminate themselves on
  * completion of executing some JavaScript.
  */
trait Engine {
  def executeJs(
    source: File, args: immutable.Seq[String], environment: Map[String, String] = Map.empty,
    stdOutSink: String => Unit, stdErrSink: String => Unit
  ): JsExecutionResult

  def isNode: Boolean
}

case class JsExecutionResult(exitValue: Int)
