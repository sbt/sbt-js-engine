package com.typesafe.sbt.jse

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import spray.json.JsonParser
import java.io.File
import xsbti.Severity
import com.typesafe.sbt.web.incremental.OpSuccess

class SbtJsTaskPluginSpec extends Specification with ScalaCheck {

  "the jstask" should {
    "Translate json OpResult/Problems properly" in {
      val p = JsonParser( s"""
          {
              "problems": [
                  {
                      "characterOffset": 5,
                      "lineContent": "a = 1",
                      "lineNumber": 1,
                      "message": "Missing semicolon.",
                      "severity": "error",
                      "source": "src/main/assets/js/a.js"
                  }
              ],
              "results": [
                  {
                      "result": {
                          "filesRead": [
                              "src/main/assets/js/a.js"
                          ],
                          "filesWritten": []
                      },
                      "source": "src/main/assets/js/a.js"
                  }
              ]
          }
      """)

      import SbtJsTask.JsTaskProtocol._
      val problemResultsPair = p.convertTo[ProblemResultsPair]
      problemResultsPair.problems.size must_== 1
      problemResultsPair.problems(0).position().offset().get() must_== 5
      problemResultsPair.problems(0).position().lineContent() must_== "a = 1"
      problemResultsPair.problems(0).position().line().get() must_== 1
      problemResultsPair.problems(0).message must_== "Missing semicolon."
      problemResultsPair.problems(0).severity must_== Severity.Error
      problemResultsPair.problems(0).position().sourceFile().get must_== new File("src/main/assets/js/a.js")
      problemResultsPair.results.size must_== 1
      val opSuccess = problemResultsPair.results(0).result.asInstanceOf[OpSuccess]
      opSuccess.filesRead.size must_== 1
      opSuccess.filesWritten.size must_== 0
      problemResultsPair.results(0).source must_== new File("src/main/assets/js/a.js")
    }

    "Write a ProblemResultsPair as json then read it and recover the original value" in {
      import SbtJsTask.JsTaskProtocol.{ProblemResultsPair, problemResultPairFormat}
      import gens._
      import helpers._

      prop { (doc: ProblemResultsPair) =>
        val roundTrip = problemResultPairFormat.read(problemResultPairFormat.write(doc))
        roundTrip must beTypedEqualTo(doc)
      }
    }
  }

}
