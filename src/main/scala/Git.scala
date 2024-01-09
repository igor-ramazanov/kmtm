import com.github.arturopala.gitignore.GitIgnore
import java.io.File
import java.nio.file.Files as JFiles
import java.nio.file.Paths
import scala.sys.process.*
import scala.util.chaining.*

class Git(private val root: File, private val files: Files):

  private def readLocal: String =
    try JFiles.readString(root.toPath().resolve(".gitignore"))
    catch _ => ""

  private def readGlobal: String =
    try JFiles.readString(
        Paths
          .get(sys.env("HOME"))
          .resolve(".config")
          .resolve("git")
          .resolve("ignore")
      )
    catch _ => ""

  private def gitignore: GitIgnore = GitIgnore
    .parse(readLocal + "\n" + readGlobal)

  def ignored(file: File): Boolean =
    val path = root
      .toPath()
      .relativize(file.toPath())
      .pipe:
        case p if p.toFile().isDirectory() => p.toString() + "/"
        case p => p.toString()
    gitignore.isIgnored(path)

  def modified(): Set[File] =
    try
      "git status --porcelain"
        .!!(ProcessLogger(_ => ()))
        .split('\n')
        .collect:
          // Doesn't work in nested dirs
          case s if s.startsWith(" M") || s.startsWith("M ") =>
            root.toPath.resolve(s.drop(3)).toFile
        .toSet
        .flatMap(files.hierarchyUp(_, root))
    catch case _ => Set.empty

  def untracked(): Set[File] =
    try
      "git status --porcelain"
        .!!(ProcessLogger(_ => ()))
        .split('\n')
        .collect:
          // Doesn't work in nested dirs
          case s if s.startsWith("??") => root.toPath.resolve(s.drop(3)).toFile
        .toSet
        .flatMap(files.hierarchyDown)
        .filter(_.isFile)
        .flatMap(files.hierarchyUp(_, root))
    catch case _ => Set.empty
