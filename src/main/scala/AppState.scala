import java.io.File
import scala.jdk.CollectionConverters.*
import scala.sys.process.*
import tui.widgets.ListWidget
import tui.widgets.ListWidget.Item
import tui.widgets.ListWidget.State
import tui.Color
import tui.Modifier
import tui.Span
import tui.Style
import tui.Text

object AppState:

  enum Mode:
    case Tree, CreateFile, CreateDir, Rename

  def apply(
      root: File,
      files: Files,
      git: Git,
      symbols: Symbols,
      onFocusChange: String,
      onEnter: String,
  ): AppState = new AppState(
    root = root,
    files = files,
    git = git,
    symbols = symbols,
    onFocusChange = onFocusChange,
    onEnter = onEnter,
    userInput = "",
    mode = Mode.Tree,
    listWidgets = Array.empty,
    items = Array((root, "")),
    focusedIndex = 0,
    openedDirs = Set.empty,
    showIgnored = false,
  )

final case class AppState private (
    val root: File,
    private val files: Files,
    private val git: Git,
    private val symbols: Symbols,
    private val onFocusChange: String,
    private val onEnter: String,
    var userInput: String,
    var mode: AppState.Mode,
    var listWidgets: Array[ListWidget.Item],
    private var items: Array[(File, String)],
    private var focusedIndex: Int,
    private var openedDirs: Set[File],
    private var showIgnored: Boolean,
):

  def listWidgetState: ListWidget.State = ListWidget
    .State(offset = 0, selected = Some(focusedIndex))

  def cancelInput(): Unit =
    mode = AppState.Mode.Tree
    userInput = ""

  def createFile(): Unit =
    import AppState.Mode.*
    mode match
      case CreateFile if userInput.isBlank() => ()
      case CreateFile =>
        val parent =
          if focused.isFile() then focused.getParentFile() else focused
        focus(files.createFile(parent, userInput))
        mode = Tree
        userInput = ""
        onFocus()
      case Tree | CreateDir | Rename => mode = CreateFile

  def createDir(): Unit =
    import AppState.Mode.*
    mode match
      case CreateDir if userInput.isBlank() => ()
      case CreateDir =>
        val parent =
          if focused.isFile() then focused.getParentFile() else focused
        focus(files.createDir(parent, userInput))
        mode = Tree
        userInput = ""
        onFocus()
      case Tree | CreateFile | Rename => mode = CreateDir

  def rename(): Unit =
    val isCharValid = (char: Char) =>
      char.isLetterOrDigit || char == '.' || char == '-' || char == '_'
    import AppState.Mode.*
    mode match
      case Rename
          if userInput.isBlank() ||
            userInput.exists(char => !isCharValid(char)) ||
            items.map((file, _) => file.getName()).toSet(userInput) => ()
      case Rename =>
        val oldFile = focused
        val newFile = focused.getParentFile().toPath().resolve(userInput).toFile()
        files.rename(oldFile, newFile)
        userInput = ""
        mode = Tree
        focus(newFile)
        onFocus()
      case Tree | CreateFile | CreateDir =>
        userInput = focused.getName()
        mode = Rename

  private def focused: File = items(focusedIndex) match
    case (file, _) => file

  def focus(file: File): Unit =
    openedDirs = openedDirs ++
      files.hierarchyUp(file, root).filter(_.isDirectory)
    refresh()
    var i = 0
    var done = false
    while !done && i < items.length do
      val (f, _) = items(i)
      if file == f then done = true else i += 1

    focusedIndex = if done then i else focusedIndex
    if done then onFocus()

  def deleteFocused(): Unit =
    files.deleteRecursively(focused)
    refresh()
    onFocus()

  def toggleIgnored(): Unit =
    showIgnored = !showIgnored
    refresh()

  def openFile(): Unit =
    val _ = onEnter.!(ProcessLogger(_ => ()))

  def toggleFocusedOrOpen(): Unit =
    if focused.isFile then openFile()
    else
      val chainDown = files.hieraryDownDirsWithOneChild(focused)
      if openedDirs(focused) then openedDirs = openedDirs -- chainDown
      else openedDirs = openedDirs ++ chainDown
      refresh()

  def refresh(): Unit =
    items = files
      .buildRecursiveTreeWithPrefixMarks(root, show, openedDirs)
      .toArray
    updateListWidgetItems()

  private def show(file: File): Boolean = !git.ignored(file) || showIgnored

  def focusNext(): Unit = focusedIndex match
    case prev if prev == items.size - 1 =>
      focusedIndex = 0
      onFocus()
    case prev =>
      focusedIndex = prev + 1
      onFocus()

  def focusPrev(): Unit = focusedIndex match
    case prev if prev == 0 =>
      focusedIndex = items.size - 1
      onFocus()
    case prev =>
      focusedIndex = prev - 1
      onFocus()

  private def onFocus(): Unit = if focused.isFile then
    val cmd = onFocusChange.replace("{}", focused.getAbsolutePath())
    val wrapped = Process(command = "zsh", arguments = List("-c", cmd))
    val _ = wrapped.!!(ProcessLogger(_ => ()))

  private def updateListWidgetItems(): Unit =

    this.listWidgets = Array.ofDim[ListWidget.Item](items.size)

    val untracked = git.untracked()
    val modified = git.modified()

    for i <- items.indices do
      val (file, prefix) = items(i)
      val (name, symbol) = getNameAndSymbol(file)
      val text = Text.from(
        Span(s"$prefix$symbol ", Style.DEFAULT),
        Span(name, getStyle(file, modified, untracked)),
      )

      val item = ListWidget.Item(text, Style.DEFAULT)
      this.listWidgets.update(i, item)

  private def getNameAndSymbol(file: File): (String, String) =
    val fullname = file.getName()
    if file.isFile then
      fullname.split("\\.") match
        case Array(name, ext) => symbols.extensions.get(ext) match
            case Some(extSymbol) => name -> extSymbol
            case None => fullname -> symbols.file
        case _ => fullname -> symbols.file
    else if openedDirs(file) then fullname -> symbols.openedDir
    else fullname -> symbols.closedDir

  private def getStyle(
      file: File,
      modified: File => Boolean,
      untracked: File => Boolean,
  ): Style =
    var style = Style.DEFAULT
    if modified(file) then style = style.fg(Color.Blue)
    if untracked(file) then style = style.fg(Color.Green)
    if git.ignored(file) then style = style.addModifier(Modifier.DIM)
    style
