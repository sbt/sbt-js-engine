package com.typesafe.sbt.jse

import sbt._
import sbt.Keys._

import scala.collection.immutable

import com.typesafe.sbt.jse.engines._
import com.typesafe.sbt.jse.npm.Npm
import com.typesafe.sbt.web.SbtWeb

import scala.concurrent.duration._
import scala.sys.process.Process
import scala.util.Try

object JsEngineImport {

  object JsEngineKeys {

    object EngineType extends Enumeration {
      val CommonNode, Node, PhantomJs, Javax, Rhino, Trireme,

      /**
        * Auto detect the best available engine to use for most common tasks - this will currently select node if
        * available, otherwise it will fall back to trireme
        */
      AutoDetect = Value
    }

    val command = SettingKey[Option[File]]("jse-command", "An optional path to the command used to invoke the engine.")
    val engineType = SettingKey[EngineType.Value]("jse-engine-type", "The type of engine to use.")
    @deprecated("No longer used", "1.3.0")
    val parallelism = SettingKey[Int]("jse-parallelism", "The number of parallel tasks for the JavaScript engine. Defaults to the # of available processors + 1 to keep things busy.")
    @deprecated("No longer used", "1.3.0")
    val npmTimeout = SettingKey[FiniteDuration]("jse-npm-timeout", "The maximum number amount of time for npm to do its thing.")
    val npmNodeModules = TaskKey[Seq[File]]("jse-npm-node-modules", "Node module files generated by NPM.")
    val npmPreferSystemInstalledNpm = SettingKey[Boolean]("jse-npm-prefer-system-installed-npm","Prefer detecting and using locally installed NPM when using a local engine that provides Node support")
    // TODO: Run install, update or ci ?
  }

}

/**
  * Declares the main parts of a WebDriver based plugin for sbt.
  */
object SbtJsEngine extends AutoPlugin {

  override def requires: Plugins = SbtWeb

  override def trigger: PluginTrigger = AllRequirements

  val autoImport: JsEngineImport.type = JsEngineImport

  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._
  import JsEngineKeys._

  /**
    * Convert an engine type enum to an engine.
    */
  def engineTypeToEngine(engineType: EngineType.Value, command: Option[File], env: Map[String, String]): Engine = {
    engineType match {
      case EngineType.CommonNode => CommonNode(command, stdEnvironment = env)
      case EngineType.Node => Node(command, stdEnvironment = env)
      case EngineType.PhantomJs => PhantomJs(command)
      case EngineType.Javax => JavaxEngine()
      case EngineType.Rhino => Rhino()
      case EngineType.Trireme => Trireme(stdEnvironment = env)
      case EngineType.AutoDetect => if (autoDetectNode) {
        Node(command, stdEnvironment = env)
      } else {
        Trireme(stdEnvironment = env)
      }
    }
  }

  private val NodeModules = "node_modules"
  private val PackageJson = "package.json"


  private lazy val autoDetectNode: Boolean = {
    val nodeExists = Try(Process("node --version").!!).isSuccess
    if (!nodeExists) {
      println("!!!")
      println("Warning: node.js detection failed, sbt will use the Rhino based Trireme JavaScript engine instead to run JavaScript assets compilation, which in some cases may be orders of magnitude slower than using node.js.")
      println("!!!")
    }
    nodeExists
  }

  private val jsEngineUnscopedSettings: Seq[Setting[_]] = Seq(
    npmNodeModules := Def.task {
      val npmDirectory = baseDirectory.value / NodeModules
      val npmPackageJson = baseDirectory.value / PackageJson
      val cacheDirectory = streams.value.cacheDirectory / "npm"
      val webJarsNodeModulesPath = (Plugin / webJarsNodeModulesDirectory).value.getCanonicalPath
      val nodePathEnv = LocalEngine.nodePathEnv(immutable.Seq(webJarsNodeModulesPath))
      val engine = engineTypeToEngine(engineType.value, command.value, nodePathEnv)
      val nodeModulesDirectory = (Plugin / webJarsNodeModulesDirectory).value
      val logger = streams.value.log
      val baseDir = baseDirectory.value
      val preferSystemInstalledNpm = npmPreferSystemInstalledNpm.value

      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) { _ =>
        if (npmPackageJson.exists) {
          val npm = new Npm(engine, Some(nodeModulesDirectory / "npm" / "lib" / "npm.js"), preferSystemNpm = preferSystemInstalledNpm)
          val result = npm.update(global = false, Seq("--prefix", baseDir.getPath), logger.info(_), logger.error(_))
          if (result.exitValue != 0) {
            sys.error("Problems with NPM resolution. Aborting build.")
          }
          npmDirectory.**(AllPassFilter).get.toSet
        } else {
          IO.delete(npmDirectory)
          Set.empty
        }
      }
      runUpdate(Set(npmPackageJson)).toSeq
    }.dependsOn(Plugin / webJarsNodeModules).value,

    nodeModuleGenerators += npmNodeModules.taskValue,
    nodeModuleDirectories += baseDirectory.value / NodeModules
  )

  private val defaultEngineType = EngineType.AutoDetect

  override def projectSettings: Seq[Setting[_]] = Seq(
    engineType := sys.props.get("sbt.jse.engineType").fold(defaultEngineType)(engineTypeStr =>
      Try(EngineType.withName(engineTypeStr)).getOrElse {
        println(s"Unknown engine type $engineTypeStr for sbt.jse.engineType. Resorting back to the default of $defaultEngineType.")
        defaultEngineType
      }),
    command := sys.props.get("sbt.jse.command").map(file),
    npmPreferSystemInstalledNpm := true,

  ) ++ inConfig(Assets)(jsEngineUnscopedSettings) ++ inConfig(TestAssets)(jsEngineUnscopedSettings)

}
