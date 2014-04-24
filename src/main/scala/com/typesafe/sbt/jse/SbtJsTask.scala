package com.typesafe.sbt.jse

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.incremental.OpInputHasher
import spray.json._
import com.typesafe.sbt.web._
import xsbti.{Problem, Severity}
import com.typesafe.sbt.web.incremental.OpResult
import com.typesafe.sbt.web.incremental.OpFailure
import com.typesafe.sbt.web.incremental.OpInputHash
import akka.actor.ActorRef
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.immutable
import com.typesafe.jse._
import com.typesafe.sbt.web.SbtWeb._
import akka.pattern.ask
import com.typesafe.sbt.web.incremental
import com.typesafe.sbt.web.CompileProblems
import com.typesafe.jse.Engine.JsExecutionResult
import com.typesafe.sbt.web.incremental.OpSuccess
import sbt.Configuration
import sbinary.{Input, Output, Format}
import scala.concurrent.duration._

object JsTaskImport {

  object JsTaskKeys {

    val fileFilter = SettingKey[FileFilter]("jstask-file-filter", "The file extension of files to perform a task on.")
    val fileInputHasher = TaskKey[OpInputHasher[File]]("jstask-file-input-hasher", "A function that constitues a change for a given file.")
    val jsOptions = TaskKey[String]("jstask-js-options", "The JSON options to be passed to the task.")
    val taskMessage = SettingKey[String]("jstask-message", "The message to output for a task")
    val shellFile = SettingKey[String]("jstask-shell-file", "The name of the file to perform a given task.")
    val shellSource = TaskKey[File]("jstask-shell-source", "The target location of the js shell script to use.")
    val timeoutPerSource = SettingKey[FiniteDuration]("jstask-timeout-per-source", "The maximum number of seconds to wait per source file processed by the JS task.")
  }

}

/**
 * The commonality of JS task execution oriented plugins is captured by this class.
 */
object SbtJsTask extends AutoPlugin {

  override def requires = SbtJsEngine

  override def trigger = AllRequirements

  val autoImport = JsTaskImport

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsEngine.autoImport._
  import JsEngineKeys._
  import autoImport._
  import JsTaskKeys._

  val jsTaskSpecificUnscopedConfigSettings = Seq(
    fileInputHasher := OpInputHasher[File](f => OpInputHash.hashString(f.getAbsolutePath + "|" + jsOptions.value)),
    resourceManaged := target.value / moduleName.value
  )

  val jsTaskSpecificUnscopedSettings =
    inConfig(Assets)(jsTaskSpecificUnscopedConfigSettings) ++
      inConfig(TestAssets)(jsTaskSpecificUnscopedConfigSettings) ++
      Seq(
        shellSource := {
          SbtWeb.copyResourceTo(
            (target in Plugin).value / moduleName.value,
            shellFile.value,
            SbtJsTask.getClass.getClassLoader,
            streams.value.cacheDirectory / "copy-resource"
          )
        }
      )

  override def projectSettings = Seq(
    jsOptions := "{}",
    timeoutPerSource := 30.seconds
  )


  /**
   * Thrown when there is an unexpected problem to do with the task's execution.
   */
  class JsTaskFailure(m: String) extends RuntimeException(m)

  /**
   * For automatic transformation of Json structures.
   */
  object JsTaskProtocol extends DefaultJsonProtocol {

    implicit object FileFormat extends JsonFormat[File] {
      def write(f: File) = JsString(f.getCanonicalPath)

      def read(value: JsValue) = value match {
        case s: JsString => new File(s.convertTo[String])
        case x => deserializationError(s"String expected for a file, instead got $x")
      }
    }

    implicit val opSuccessFormat = jsonFormat2(OpSuccess)

    implicit object LineBasedProblemFormat extends JsonFormat[LineBasedProblem] {
      def write(p: LineBasedProblem) = JsString("unimplemented")

      def read(value: JsValue) = value match {
        case o: JsObject => new LineBasedProblem(
          o.fields.get("message").map(_.convertTo[String]).getOrElse("unknown message"),
          o.fields.get("severity").map {
            v =>
              v.toString() match {
                case "info" => Severity.Info
                case "warn" => Severity.Warn
                case _ => Severity.Error
              }
          }.getOrElse(Severity.Error),
          o.fields.get("lineNumber").map(_.convertTo[Int]).getOrElse(0),
          o.fields.get("characterOffset").map(_.convertTo[Int]).getOrElse(0),
          o.fields.get("lineContent").map(_.convertTo[String]).getOrElse("unknown line content"),
          o.fields.get("source").map(_.convertTo[File]).getOrElse(file(""))
        )
        case x => deserializationError(s"Object expected for the problem, instead got $x")
      }

    }

    implicit object OpResultFormat extends JsonFormat[OpResult] {

      def write(r: OpResult) = JsString("unimplemented")

      def read(value: JsValue) = value match {
        case o: JsObject => opSuccessFormat.read(o)
        case JsNull => OpFailure
        case x => deserializationError(s"Object expected for the op result, instead got $x")
      }
    }

    case class ProblemResultsPair(results: Seq[SourceResultPair], problems: Seq[LineBasedProblem])

    case class SourceResultPair(result: OpResult, source: File)

    implicit val sourceResultPairFormat = jsonFormat2(SourceResultPair)
    implicit val problemResultPairFormat = jsonFormat2(ProblemResultsPair)
  }

  // node.js docs say *NOTHING* about what encoding is used when you write a string to stdout.
  // It seems that they have it hard coded to use UTF-8, some small tests I did indicate that changing the platform
  // encoding makes no difference on what encoding node uses when it writes strings to stdout.
  private val NodeEncoding = "UTF-8"
  // Used to signal when the script is sending back structured JSON data
  private val JsonEscapeChar: Char = 0x10

  private type FileOpResultMappings = Map[File, OpResult]

  private def FileOpResultMappings(s: (File, OpResult)*): FileOpResultMappings = Map(s: _*)

  private type FileWrittenAndProblems = (Boolean, Seq[File], Seq[Problem])

  private def FilesWrittenAndProblems(fullRun: Boolean): FileWrittenAndProblems = FilesWrittenAndProblems(fullRun, Nil, Nil)

  private def FilesWrittenAndProblems(filesWrittenAndProblems: (Boolean, Seq[File], Seq[Problem])): FileWrittenAndProblems = filesWrittenAndProblems


  private def executeJsOnEngine(engine: ActorRef, shellSource: File, args: Seq[String],
                                stderrSink: String => Unit, stdoutSink: String => Unit)
                               (implicit timeout: Timeout, ec: ExecutionContext): Future[Seq[JsValue]] = {

    (engine ? Engine.ExecuteJs(
      shellSource,
      args.to[immutable.Seq],
      timeout.duration
    )).mapTo[JsExecutionResult].map {
      result =>

      // Stuff below probably not needed once jsengine is refactored to stream this

      // Dump stderr as is
        if (!result.error.isEmpty) {
          stderrSink(new String(result.error.toArray, NodeEncoding))
        }

        // Split stdout into lines
        val outputLines = new String(result.output.toArray, NodeEncoding).split("\r?\n")

        // Iterate through lines, extracting out JSON messages, and printing the rest out
        val results = outputLines.foldLeft(Seq.empty[JsValue]) {
          (results, line) =>
            if (line.indexOf(JsonEscapeChar) == -1) {
              stdoutSink(line)
              results
            } else {
              val (out, json) = line.span(_ != JsonEscapeChar)
              if (!out.isEmpty) {
                stdoutSink(out)
              }
              results :+ JsonParser(json.drop(1))
            }
        }

        if (result.exitValue != 0) {
          throw new JsTaskFailure(new String(result.error.toArray, NodeEncoding))
        }
        results
    }

  }

  private def executeSourceFilesJs(
                                    engine: ActorRef,
                                    shellSource: File,
                                    sourceFileMappings: Seq[PathMapping],
                                    target: File,
                                    options: String,
                                    stderrSink: String => Unit,
                                    stdoutSink: String => Unit
                                    )(implicit timeout: Timeout): Future[(FileOpResultMappings, Seq[Problem])] = {

    import ExecutionContext.Implicits.global

    val args = immutable.Seq(
      JsArray(sourceFileMappings.map(x => JsArray(JsString(x._1.getCanonicalPath), JsString(x._2))).toList).toString(),
      JsString(target.getAbsolutePath).toString(),
      options
    )

    executeJsOnEngine(engine, shellSource, args, stderrSink, stdoutSink).map {
      results =>
        import JsTaskProtocol._
        val prp = results.foldLeft(ProblemResultsPair(Nil, Nil)) {
          (cumulative, result) =>
            val prp = result.convertTo[ProblemResultsPair]
            ProblemResultsPair(
              cumulative.results ++ prp.results,
              cumulative.problems ++ prp.problems
            )
        }
        (prp.results.map(sr => sr.source -> sr.result).toMap, prp.problems)
    }
  }

  /*
   * For reading/writing binary representations of files.
   */
  private implicit object FileFormat extends Format[File] {

    import sbinary.DefaultProtocol._

    def reads(in: Input): File = file(StringFormat.reads(in))

    def writes(out: Output, fh: File) = StringFormat.writes(out, fh.getAbsolutePath)
  }

  /**
   * Primary means of executing a JavaScript shell script for processing source files. unmanagedResources is assumed
   * to contain the source files to filter on.
   * @param task The task to resolve js task settings from - relates to the concrete plugin sub class
   * @param config The sbt configuration to use e.g. Assets or TestAssets
   * @return A task object
   */
  def jsSourceFileTask(
                        task: TaskKey[Seq[File]],
                        config: Configuration
                        ): Def.Initialize[Task[Seq[File]]] = Def.task {

    val nodeModulePaths = (nodeModuleDirectories in Plugin).value.map(_.getCanonicalPath)
    val engineProps = SbtJsEngine.engineTypeToProps((engineType in task).value, NodeEngine.nodePathEnv(nodeModulePaths.to[immutable.Seq]))

    val sources = ((unmanagedSources in config).value ** (fileFilter in task in config).value).get

    val logger: Logger = state.value.log

    implicit val opInputHasher = (fileInputHasher in task in config).value
    val results: FileWrittenAndProblems = incremental.runIncremental((streams in config).value.cacheDirectory / "run", sources) {
      modifiedSources: Seq[File] =>

        val fullRun = modifiedSources.size == sources.size

        if (modifiedSources.size > 0) {

          streams.value.log.info(s"${(taskMessage in task in config).value} on ${
            modifiedSources.size
          } source(s)")

          val resultBatches: Seq[Future[(FileOpResultMappings, Seq[Problem])]] =
            try {
              val sourceBatches = (modifiedSources grouped Math.max(modifiedSources.size / (parallelism in task).value, 1)).toSeq
              sourceBatches.map {
                sourceBatch =>
                  implicit val timeout = Timeout((timeoutPerSource in task in config).value * sourceBatch.size)
                  withActorRefFactory(state.value, this.getClass.getName) {
                    arf =>
                      val engine = arf.actorOf(engineProps)
                      implicit val timeout = Timeout((timeoutPerSource in task in config).value * sourceBatch.size)
                      executeSourceFilesJs(
                        engine,
                        (shellSource in task in config).value,
                        sourceBatch.pair(relativeTo((unmanagedSourceDirectories in config).value)),
                        (resourceManaged in task in config).value,
                        (jsOptions in task in config).value,
                        m => logger.error(m),
                        m => logger.info(m)
                      )
                  }
              }
            }

          import scala.concurrent.ExecutionContext.Implicits.global
          val pendingResults = Future.sequence(resultBatches)
          val completedResults = Await.result(pendingResults, (timeoutPerSource in task in config).value * modifiedSources.size)

          completedResults.foldLeft((FileOpResultMappings(), FilesWrittenAndProblems(fullRun))) {
            (allCompletedResults, completedResult) =>

              val (prevOpResults, (_, prevFilesWritten, prevProblems)) = allCompletedResults

              val (nextOpResults, nextProblems) = completedResult
              val nextFilesWritten: Seq[File] = nextOpResults.values.map {
                case opSuccess: OpSuccess => opSuccess.filesWritten
                case _ => Nil
              }.flatten.toSeq

              (
                prevOpResults ++ nextOpResults,
                FilesWrittenAndProblems(fullRun, prevFilesWritten ++ nextFilesWritten, prevProblems ++ nextProblems)
                )
          }

        } else {
          (FileOpResultMappings(), FilesWrittenAndProblems(fullRun))
        }
    }

    val (fullRun, filesWritten, problems) = results

    CompileProblems.report((reporter in task).value, problems)

    import Cache._
    val previousMappings = if (fullRun) Nil else (task in config).previous.getOrElse(Nil)
    val untouchedMappings = previousMappings.toSet -- filesWritten
    untouchedMappings.filter(_.exists).toSeq ++ filesWritten
  }

  private def addUnscopedJsSourceFileTasks(sourceFileTask: TaskKey[Seq[File]]): Seq[Setting[_]] = {
    Seq(
      resourceGenerators <+= sourceFileTask,
      managedResourceDirectories += (resourceManaged in sourceFileTask).value
    )
  }

  /**
   * Convenience method to add a source file task into the Asset and TestAsset configurations, along with adding the
   * source file tasks in to their respective collection.
   * @param sourceFileTask The task key to declare.
   * @return The settings produced.
   */
  def addJsSourceFileTasks(sourceFileTask: TaskKey[Seq[File]]): Seq[Setting[_]] = {
    Seq(
      sourceFileTask in Assets := jsSourceFileTask(sourceFileTask, Assets).dependsOn(nodeModules in Plugin).value,
      sourceFileTask in TestAssets := jsSourceFileTask(sourceFileTask, TestAssets).dependsOn(nodeModules in Plugin).value,
      resourceManaged in sourceFileTask in Assets := webTarget.value / sourceFileTask.key.label / "main",
      resourceManaged in sourceFileTask in TestAssets := webTarget.value / sourceFileTask.key.label / "test",
      sourceFileTask := (sourceFileTask in Assets).value
    ) ++
      inConfig(Assets)(addUnscopedJsSourceFileTasks(sourceFileTask)) ++
      inConfig(TestAssets)(addUnscopedJsSourceFileTasks(sourceFileTask))
  }

  /**
   * Execute some arbitrary JavaScript.
   *
   * This method is intended to assist in building SBT tasks that execute generic JavaScript.  For example:
   *
   * {{{
   * myTask := {
   *   executeJs(state.value, engineType.value, Seq((nodeModules in Plugin).value.getCanonicalPath,
   *     baseDirectory.value / "path" / "to" / "myscript.js", Seq("arg1", "arg2"), 30.seconds)
   * }
   * }}}
   *
   * @param state The SBT state.
   * @param engineType The type of engine to use.
   * @param nodeModules The node modules to provide (if the JavaScript engine in use supports this).
   * @param shellSource The script to execute.
   * @param args The arguments to pass to the script.
   * @param timeout The maximum amount of time to wait for the script to finish.
   * @return A JSON status object if one was sent by the script.  A script can send a JSON status object by, as the
   *         last thing it does, sending a DLE character (0x10) followed by some JSON to std out.
   */
  def executeJs(
                 state: State,
                 engineType: EngineType.Value,
                 nodeModules: Seq[String],
                 shellSource: File,
                 args: Seq[String],
                 timeout: FiniteDuration
                 ): Seq[JsValue] = {
    val engineProps = SbtJsEngine.engineTypeToProps(engineType, NodeEngine.nodePathEnv(nodeModules.to[immutable.Seq]))

    withActorRefFactory(state, this.getClass.getName) {
      arf =>
        val engine = arf.actorOf(engineProps)
        implicit val t = Timeout(timeout)
        import ExecutionContext.Implicits.global
        Await.result(
          executeJsOnEngine(engine, shellSource, args, m => state.log.error(m), m => state.log.info(m)),
          timeout
        )
    }
  }

}
