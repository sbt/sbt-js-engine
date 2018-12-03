package com.typesafe.sbt.jse.engines

import org.specs2.mutable.Specification
import java.io.File
import scala.collection.immutable

class RhinoSpec extends Specification {

  "The Rhino engine" should {

    "execute some javascript by passing in a string arg and comparing its return value" in {
      val f = new File(classOf[RhinoSpec].getResource("test-rhino.js").toURI)
      val out = new StringBuilder
      val err = new StringBuilder
      Rhino().executeJs(f, immutable.Seq("999"), Map.empty, out.append(_), err.append(_))
      err.toString.trim must_== ""
      out.toString.trim must_== "Hello 999"
    }

    "execute some javascript by passing in a string arg and comparing its return value expecting an error" in {
      val f = new File(classOf[RhinoSpec].getResource("test-node.js").toURI)
      val out = new StringBuilder
      val err = new StringBuilder
      Rhino().executeJs(f, immutable.Seq("999"), Map.empty, out.append(_), err.append(_))
      out.toString.trim must_== ""
      err.toString.trim must contain("""Error: Module "console" not found""")
    }

  }
}
