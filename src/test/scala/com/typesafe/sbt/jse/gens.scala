package com.typesafe.sbt.jse

import java.io.File
import xsbti.Severity
import com.typesafe.sbt.jse.SbtJsTask.JsTaskProtocol.{SourceResultPair, ProblemResultsPair}
import com.typesafe.sbt.web.incremental.{OpResult, OpSuccess, OpFailure}
import com.typesafe.sbt.web.LineBasedProblem
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary



object gens {
  import Gen._

  implicit val fileArb = Arbitrary(fileGen)
  implicit val opResultArb = Arbitrary(opResultGen)
  implicit val lineBasedProblemArb = Arbitrary(lineBasedProblemGen)
  implicit val sourceResultPairArb = Arbitrary(sourceResultPairGen)
  implicit val problemResultsPairArb = Arbitrary(problemResultsPairGen)

  val directoryNameGen: Gen[String] =
    for {
      numChars <- choose(1, 40)
      chars <- listOfN(numChars, alphaNumChar)
    } yield chars.map(_.toString).mkString

  val fileGen: Gen[File] =
    for {
      depth <- choose(0, 20)
      directories <- listOfN(depth, directoryNameGen)
    } yield new File(directories.mkString(File.separator, File.separator, "")).getCanonicalFile

  val opResultGen: Gen[OpResult] = oneOf(const(OpFailure), resultOf(OpSuccess.apply _))

  val severityGen: Gen[Severity] = oneOf(Severity.Info, Severity.Warn, Severity.Error)

  val lineBasedProblemGen: Gen[LineBasedProblem] =
    for {
      message <- arbitrary[String]
      severity <- severityGen
      lineNumber <- arbitrary[Int]
      characterOffset <- arbitrary[Int]
      lineContent <- arbitrary[String]
      source <- fileGen
    } yield new LineBasedProblem(message, severity, lineNumber, characterOffset, lineContent, source)

  val sourceResultPairGen: Gen[SourceResultPair] =
    resultOf(SourceResultPair.apply _)

  val problemResultsPairGen: Gen[ProblemResultsPair] =
    for {
      numResults <- choose(0, 10)
      numProblems <- choose(0, 20)
      results <- containerOfN[Seq, SourceResultPair](numResults, sourceResultPairGen)
      problems <- containerOfN[Seq, LineBasedProblem](numProblems, lineBasedProblemGen)
    } yield ProblemResultsPair(results, problems)

}
