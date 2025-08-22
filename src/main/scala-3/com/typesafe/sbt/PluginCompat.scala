package com.typesafe.sbt.jse

import java.nio.file.{ Path => NioPath }
import java.io.{ File => IoFile }
import sbt.*
import sbt.Keys.Classpath
import xsbti.{ FileConverter, HashedVirtualFileRef, VirtualFileRef }
import com.typesafe.sbt.web.PathMapping

private[sbt] object PluginCompat:
  export sbt.CacheImplicits.{ *, given }
  export sbt.util.cacheLevel
  export sbt.Def.uncached

  val TestResultPassed = TestResult.Passed
  type FileRef = HashedVirtualFileRef
  type UnhashedFileRef = VirtualFileRef

  def toNioPath(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): NioPath =
    conv.toPath(a.data)
  inline def toFile(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): File =
    toNioPath(a).toFile
  def toSet[A](iterable: Iterable[A]): Set[A] = iterable.to(Set)
  def toNioPath(hvf: VirtualFileRef)(using conv: FileConverter): NioPath =
    conv.toPath(hvf)
  def toFile(hvf: VirtualFileRef)(using conv: FileConverter): File =
    toNioPath(hvf).toFile
  inline def toFileRef(file: File)(using conv: FileConverter): FileRef =
    conv.toVirtualFile(file.toPath)
end PluginCompat