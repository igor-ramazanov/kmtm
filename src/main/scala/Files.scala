import java.io.File
import java.nio.file.Files as JFiles
import java.nio.file.Path
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set as MutableSet
import scala.collection.mutable.Stack
import scala.jdk.CollectionConverters.*

class Files(symbols: Symbols):

  private def walk(
      start: File,
      action: File => Unit,
      condition: File => Boolean,
      explode: File => Iterator[File],
  ): Unit =
    val stack = Stack(start)
    while stack.nonEmpty do
      val file = stack.pop()
      action(file)
      if condition(file) then
        val children = explode(file).toArray
        var i = children.length - 1
        while i != -1 do
          stack.push(children(i))
          i -= 1

  def deleteRecursively(file: File): Unit =
    val acc = ListBuffer.empty[File]
    walk(
      start = file,
      action = file => acc += file,
      condition = file => file.isDirectory(),
      explode = file =>
        JFiles.list(file.toPath()).map(path => path.toFile()).iterator().asScala,
    )
    acc.reverseIterator.foreach(file => JFiles.delete(file.toPath()))

  def hierarchyUp(startFrom: File, stopAt: File): Set[File] =
    val acc = MutableSet.empty[File]
    walk(
      start = startFrom,
      action = file => acc += file,
      condition = file => file != stopAt,
      explode = file => List(file.getParentFile()).iterator,
    )
    acc.toSet

  def hieraryDownDirsWithOneChild(file: File): Set[File] =
    val acc = MutableSet.empty[File]
    walk(
      start = file,
      action = file => acc += file,
      condition =
        file => file.isDirectory() && JFiles.list(file.toPath()).count() == 1,
      explode = file =>
        JFiles
          .list(file.toPath())
          .iterator()
          .asScala
          .take(1)
          .map(path => path.toFile()),
    )
    acc.toSet

  def hierarchyDown(startFrom: File): Set[File] =
    val acc = MutableSet.empty[File]
    walk(
      start = startFrom,
      action = file => acc += file,
      condition = file => file.isDirectory(),
      explode = file =>
        JFiles.list(file.toPath()).map(path => path.toFile()).iterator().asScala,
    )
    acc.toSet

  def buildRecursiveTreeWithPrefixMarks(
      root: File,
      show: File => Boolean,
      opened: File => Boolean,
  ): ListBuffer[(File, String)] =
    val result = ListBuffer.empty[(File, String)]
    val prefixes = Stack.empty[Boolean]
    val stack = Stack(Iterator(root))
    while stack.nonEmpty do
      if stack.top.hasNext then
        val current = stack.top.next()
        val isLast = !stack.top.hasNext
        val isRoot = current == root
        if show(current) then
          val prefix = renderPrefix(isRoot, isLast, prefixes)
          result += current -> prefix
          if current.isDirectory() && opened(current) then
            stack.push(children(current))
            if !isRoot then prefixes.addOne(isLast)
      else
        val _ = prefixes.removeLastOption()
        val _ = stack.pop()
    result

  private def renderPrefix(
      isRoot: Boolean,
      isLast: Boolean,
      prefixes: Stack[Boolean],
  ): String =
    if isRoot then ""
    else
      prefixes.map(b => if b then symbols.empty else symbols.pipe).mkString +
        (if isLast then symbols.lastChild else symbols.child)

  private def children(parent: File) = JFiles
    .list(parent.toPath)
    .toList()
    .asScala
    .sortWith: (l, r) =>
      (JFiles.isDirectory(l), JFiles.isDirectory(r)) match
        case (true, false) => true
        case (false, true) => false
        case _ => l.toAbsolutePath.toString < r.toAbsolutePath.toString
    .iterator
    .map(path => path.toFile())

  def createDir(parent: File, name: String): File = JFiles
    .createDirectory(parent.toPath().resolve(name))
    .toFile()

  def createFile(parent: File, name: String): File = JFiles
    .createFile(parent.toPath().resolve(name))
    .toFile()

  def rename(oldFile: File, newFile: File): Unit =
    val _ = JFiles.move(oldFile.toPath(), newFile.toPath())
