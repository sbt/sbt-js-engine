package com.typesafe.sbt.jse

import com.typesafe.sbt.jse.SbtJsTask.JsTaskProtocol.{ProblemResultsPair, SourceResultPair}
import com.typesafe.sbt.web.LineBasedProblem

object helpers {

  // Define a custom equality function for LineBasedProblem
  def lineBasedProblemEquality(p1: LineBasedProblem, p2: LineBasedProblem): Boolean =
    p1.message == p2.message &&
      p1.severity == p2.severity &&
      p1.position.line == p2.position.line &&
      p1.position.lineContent == p2.position.lineContent &&
      p1.position.offset == p2.position.offset &&
      p1.position.sourceFile.get.getCanonicalPath == p2.position.sourceFile.get.getCanonicalPath

  // Define a custom equality function for SourceResultPair
  def sourceResultPairEquality(p1: SourceResultPair, p2: SourceResultPair): Boolean =
    p1.result == p2.result &&
      p1.source.getCanonicalPath == p2.source.getCanonicalPath

  private def stringifyLineBasedProblem(p: LineBasedProblem): String =
    s"""|LineBasedProblem(
        |  ${p.message}
        |  ${p.severity}
        |  ${p.position.line.get}
        |  ${p.position.lineContent}
        |  ${p.position.offset.get}
        |  ${p.position.sourceFile.get}
        |)""".stripMargin

  private def stringifyProblemResultsPair(p: ProblemResultsPair): String =
    s"""|ProblemResultsPair(
        |  ${p.results}
        |  ${p.problems.map(stringifyLineBasedProblem)}
        |)""".stripMargin
}