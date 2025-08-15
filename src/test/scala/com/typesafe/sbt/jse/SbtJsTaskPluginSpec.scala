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

      import SbtJsTask.JsTaskProtocol.*
      val problemResultsPair = p.convertTo[ProblemResultsPair]
      problemResultsPair.problems.size must_== 1
      problemResultsPair.problems.head.position().offset().get() must_== 5
      problemResultsPair.problems.head.position().lineContent() must_== "a = 1"
      problemResultsPair.problems.head.position().line().get() must_== 1
      problemResultsPair.problems.head.message must_== "Missing semicolon."
      problemResultsPair.problems.head.severity must_== Severity.Error
      problemResultsPair.problems.head.position().sourceFile().get must_== new File("src/main/assets/js/a.js")
      problemResultsPair.results.size must_== 1
      val opSuccess = problemResultsPair.results.head.result.asInstanceOf[OpSuccess]
      opSuccess.filesRead.size must_== 1
      opSuccess.filesWritten.size must_== 0
      problemResultsPair.results.head.source must_== new File("src/main/assets/js/a.js")
    }

    "Write a ProblemResultsPair as json then read it and recover the original value" in {
      import SbtJsTask.JsTaskProtocol.{ProblemResultsPair, problemResultPairFormat}
      import gens.*
      import helpers.*


      prop { (doc: ProblemResultsPair) =>
        val roundTrip = problemResultPairFormat.read(problemResultPairFormat.write(doc))

        roundTrip.results must containTheSameElementsAs(doc.results, sourceResultPairEquality)
        roundTrip.problems must containTheSameElementsAs(doc.problems, lineBasedProblemEquality)
      }
    }
  }

}
