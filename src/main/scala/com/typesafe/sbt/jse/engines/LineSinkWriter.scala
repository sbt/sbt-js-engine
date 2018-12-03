package com.typesafe.sbt.jse.engines

import java.io.{OutputStream, Writer}

private[jse] class LineSinkWriter(override protected val writeLine: String => Unit) extends Writer with LineSink {

  override def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
    writeLine(new String(cbuf, off, len))
  }

  override def flush(): Unit = ()

  override def close(): Unit = ()
}

private[jse] class LineSinkOutputStream(override protected val writeLine: String => Unit) extends OutputStream with LineSink {

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    // Technically unsafe since it won't handle split surrogates, however, in practice, complete characters should only
    // ever be written.
    writeLine(new String(b, off, len, "utf-8"))
  }

  override def write(b: Int): Unit = write(Array(b.asInstanceOf[Byte]))
}

private[jse] trait LineSink {

  protected val writeLine: String => Unit

  private var currentLine = ""

  protected def onWrite(str: String): Unit = synchronized {
    val buffer = if (currentLine.isEmpty) str else currentLine + str
    val lines = buffer.split("\r?\n", -1)
    currentLine = lines.last
    if (lines.length > 1) {
      for (i <- 0 until lines.length - 1) {
        writeLine(lines(i))
      }
    }
  }

}