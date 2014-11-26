package com.typesafe.sbt.jse

import com.typesafe.sbt.web.LineBasedProblem
import com.typesafe.sbt.jse.SbtJsTask.JsTaskProtocol.ProblemResultsPair



object helpers {

  def areEqualLineBasedProblems(p1: LineBasedProblem, p2: LineBasedProblem): Boolean =
    p1.message == p2.message &&
    p1.severity == p2.severity &&
    p1.position.line.get == p2.position.line.get &&
    p1.position.lineContent == p2.position.lineContent &&
    p1.position.offset.get == p2.position.offset.get &&
    p1.position.sourceFile.get == p2.position.sourceFile.get

  def areEqualProblemResultsPairs(p1: ProblemResultsPair, p2: ProblemResultsPair): Boolean =
    p1.results == p2.results &&
    p1.problems.zip(p2.problems).forall(x => areEqualLineBasedProblems(x._1, x._2))

  def stringifyLineBasedProblem(p: LineBasedProblem): String =
    s"""|LineBasedProblem(
        |  ${p.message}
        |  ${p.severity}
        |  ${p.position.line.get}
        |  ${p.position.lineContent}
        |  ${p.position.offset.get}
        |  ${p.position.sourceFile.get}
        |)""".stripMargin

  def stringifyProblemResultsPair(p: ProblemResultsPair): String =
    s"""|ProblemResultsPair(
        |  ${p.results}
        |  ${p.problems.map(stringifyLineBasedProblem _)}
        |)""".stripMargin

}
