package com.typesafe.sbt.jse.engines

import org.specs2.mutable.Specification
import java.io.File
import scala.collection.immutable

class TriremeSpec extends Specification {

  sequential

  "The Trireme engine" should {
    "execute some javascript by passing in a string arg and comparing its return value" in {
      val f = new File(classOf[TriremeSpec].getResource("test-node.js").toURI)
      val out = new StringBuilder
      val err = new StringBuilder
      Trireme().executeJs(f, immutable.Seq("999"), Map.empty, out.append(_), err.append(_))
      err.toString.trim must_== ""
      out.toString.trim must_== "999"
    }

    "execute some javascript by passing in a string arg and comparing its return value expecting an error" in {
      val f = new File(classOf[TriremeSpec].getResource("test-rhino.js").toURI)
      val out = new StringBuilder
      val err = new StringBuilder
      Trireme().executeJs(f, immutable.Seq("999"), Map.empty, out.append(_), err.append(_))
      out.toString.trim must_== ""
      err.toString.trim must startWith("""ReferenceError: "readFile" is not defined""")
    }
  }

  private def runSimpleTest() = {
    val f = new File(classOf[TriremeSpec].getResource("test-node.js").toURI)
    val out = new StringBuilder
    val err = new StringBuilder
    Trireme().executeJs(f, immutable.Seq("999"), Map.empty, out.append(_), err.append(_))
    err.toString.trim must_== ""
    out.toString.trim must_== "999"
  }

  "not leak threads" in {
    // this test assumes that there are no other trireme tests running concurrently, if there are, the trireme thread
    // count will be non 0
    runSimpleTest()

    Thread.sleep(1)

    import scala.collection.JavaConverters._
    val triremeThreads = Thread.getAllStackTraces.keySet.asScala
      .filter(_.getName.contains("Trireme"))

    ("trireme threads: " + triremeThreads) <==> (triremeThreads.size === 0)
    ok
  }

  "not leak file descriptors" in {
    import java.lang.management._
    val os = ManagementFactory.getOperatingSystemMXBean
    try {
      // To get the open file descriptor count, need to check if it exists
      os.getClass.getMethod("getOpenFileDescriptorCount")

      // brew a little first
      runSimpleTest()
      runSimpleTest()
      runSimpleTest()
      runSimpleTest()

      val openFds = UnixGetOpenFileDescriptors.getCount()
      runSimpleTest()

      UnixGetOpenFileDescriptors.getCount() must_== openFds
    } catch {
      case _: NoSuchMethodException =>
        println("Skipping file descriptor leak test because OS mbean doesn't have getOpenFileDescriptorCount")
        ok
    }
  }
}

// This is in a separate object so that it only gets loaded when we call it, avoiding class not found
// in Windows
object UnixGetOpenFileDescriptors {
  import java.lang.management.ManagementFactory
  import com.sun.management.UnixOperatingSystemMXBean

  private val os = ManagementFactory.getOperatingSystemMXBean
  def getCount(): Long = {
    os.asInstanceOf[UnixOperatingSystemMXBean]
      .getMaxFileDescriptorCount
  }
}
