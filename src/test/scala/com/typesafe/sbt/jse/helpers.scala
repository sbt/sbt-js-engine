package com.typesafe.sbt.jse

import com.typesafe.sbt.jse.SbtJsTask.JsTaskProtocol.ProblemResultsPair
import com.typesafe.sbt.web.LineBasedProblem
import org.specs2.matcher.describe._


object helpers {

  implicit val problemResultsPairDiffable: Diffable[ProblemResultsPair] = new Diffable[ProblemResultsPair] {
    override def diff(actual: ProblemResultsPair, expected: ProblemResultsPair): ComparisonResult = {
      if (areEqualProblemResultsPairs(actual, expected)) {
        OtherIdentical(actual)
      } else {
        new DifferentComparisonResult {
          def render: String = stringifyProblemResultsPair(actual) + " != " + stringifyProblemResultsPair(expected)
        }
      }

    }
  }

  private def areEqualLineBasedProblems(p1: LineBasedProblem, p2: LineBasedProblem): Boolean =
    p1.message == p2.message &&
    p1.severity == p2.severity &&
    p1.position.line.get == p2.position.line.get &&
    p1.position.lineContent == p2.position.lineContent &&
    p1.position.offset.get == p2.position.offset.get &&
    p1.position.sourceFile.get == p2.position.sourceFile.get

  private def areEqualProblemResultsPairs(p1: ProblemResultsPair, p2: ProblemResultsPair): Boolean =
    p1.results == p2.results &&
    p1.problems.zip(p2.problems).forall(x => areEqualLineBasedProblems(x._1, x._2))

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
        |  ${p.problems.map(stringifyLineBasedProblem _)}
        |)""".stripMargin
}
