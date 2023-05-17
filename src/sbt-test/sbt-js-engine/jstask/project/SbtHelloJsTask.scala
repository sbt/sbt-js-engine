package com.hello

import sbt._
import sbt.Keys._
import sbt.File
import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.web.SbtWeb
import spray.json.{JsBoolean, JsObject}

object Import {

  object HelloJsTaskKeys {

    val hello = TaskKey[Seq[File]]("hello", "Perform JavaScript linting.")

    val compress = SettingKey[Boolean]("hello-compress", "Write to a .min.js instead of a .js.")
    val fail = SettingKey[Boolean]("hello-fail", "Cause a problem to be generated for each source file.")
  }

}

/**
 * The sbt plugin plumbing around the JSHint library.
 */
object SbtHelloJsTask extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport.HelloJsTaskKeys._

  val helloJsTaskUnscopedSettings = Seq(
    includeFilter := jsFilter.value,
    jsOptions := JsObject(
      "compress" -> JsBoolean(compress.value),
      "fail" -> JsBoolean(fail.value)
    ).toString()
  )

  override def buildSettings = inTask(hello)(
    SbtJsTask.jsTaskSpecificUnscopedBuildSettings ++ Seq(
      moduleName := "hello",
      shellFile := SbtHelloJsTask.getClass.getClassLoader.getResource("hello-shell.js")
    )
  )

  override def projectSettings = Seq(
    compress := false,
    fail := false
  ) ++
    inTask(hello)(
      SbtJsTask.jsTaskSpecificUnscopedProjectSettings ++
        inConfig(Assets)(helloJsTaskUnscopedSettings) ++
        inConfig(TestAssets)(helloJsTaskUnscopedSettings) ++
        Seq(
          (Assets / taskMessage)  := "Saying hello",
          (TestAssets / taskMessage)  := "Saying hello test"

        )
    ) ++ SbtJsTask.addJsSourceFileTasks(hello)

}
