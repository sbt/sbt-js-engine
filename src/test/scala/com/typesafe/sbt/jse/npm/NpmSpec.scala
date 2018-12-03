package com.typesafe.sbt.jse.npm


import java.io.File

import com.typesafe.sbt.jse.engines.Node
import org.specs2.mutable.Specification
import sbt.io.IO

class NpmSpec extends Specification {

  "Npm" should {
    "perform an update, retrieve resources, and execute node-gyp (the native compilation tool)" in {

      // cleanup from any past tests
      IO.delete(new File("node_modules"))

      val out = new StringBuilder
      val err = new StringBuilder

      val to = new File(new File("target"), "webjars")
      val cacheFile = new File(to, "extraction-cache")
      val npm = new Npm(Node(), NpmLoader.load(to, cacheFile, this.getClass.getClassLoader), verbose = true)
      val result = npm.update(false, Nil, s => {
        println("stdout: " + s)
        out.append(s + "\n")
      }, s => {
        println("stderr: " + s)
        err.append(s + "\n")
      })

      val stdErr = err.toString()
      val stdOut = out.toString()

      result.exitValue must_== 0
      stdErr must contain("npm http request GET https://registry.npmjs.org/amdefine")
    }
  }
}
