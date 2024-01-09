import cats.syntax.contravariantSemigroupal.*
import com.monovore.decline.Opts
import java.nio.file.Path

object Cli:

  private val rootOpt = Opts
    .option[Path](
      long = "root",
      help = "Root project directory.",
      short = "r",
      metavar = "file",
    )
    .map(_.toFile)
    .validate("Must exist and be a git project"): file =>
      file.exists && file.isDirectory

  private val focusedOpt = Opts
    .option[Path](
      long = "focused",
      help = "File or directory currently focused with the root directory.",
      short = "f",
      metavar = "file",
    )
    .map(_.toFile.getAbsoluteFile)
    .validate("Provided focused file doesn't exist")(_.exists)
    .orNone

  private val onFocusChangeOpt = Opts.option[String](
    long = "on-focus-change",
    help = "A command to run when the focus is moved to another file, '{}' is a placeholder for the absolute file path.",
  )

  private val onEnterOpt = Opts.option[String](
    long = "on-enter",
    help = "A command to run when the file is opened.",
  )

  val name = "kmtm"
  val header = "Simple filetree"

  val run = (rootOpt, focusedOpt, onFocusChangeOpt, onEnterOpt)
    .mapN: (root, focused, onFocusChange, onEnter) =>
      Terminal.run(root, focused, onFocusChange, onEnter)
