package com.typesafe.sbt.jse

import sbt.*
import sbt.Keys.Classpath
import com.typesafe.sbt.web.PathMapping
import xsbti.FileConverter

import java.nio.file.{ Path => NioPath }

private[sbt] object PluginCompat {
  type FileRef = java.io.File
  type UnhashedFileRef = java.io.File

  class cacheLevel(include: Array[Any]) extends annotation.StaticAnnotation
  def uncached[T](value: T): T = value

  def toFile(a: Attributed[File])(implicit conv: FileConverter): File =
    a.data
  def toSet[A](iterable: Iterable[A]): Set[A] = iterable.to[Set]
  def toFile(f: File)(implicit conv: FileConverter): File = f
  def toFileRef(f: File)(implicit conv: FileConverter): FileRef = f
}