import java.io.File
import tui.crossterm.CrosstermJni
import tui.widgets.BlockWidget
import tui.widgets.ListWidget
import tui.widgets.ParagraphWidget
import tui.withTerminal
import tui.Alignment
import tui.Borders
import tui.Constraint
import tui.Corner
import tui.Direction
import tui.Frame
import tui.Grapheme
import tui.Layout
import tui.Margin
import tui.Modifier
import tui.Rect
import tui.Span
import tui.Spans
import tui.Style
import tui.Terminal
import tui.Text

object Terminal:

  private var running = true

  def run(
      root: File,
      focused: Option[File],
      onFocusChange: String,
      onEnter: String,
  ): Unit = withTerminal: (crosstermJni, terminal) =>
    val state = init(root, focused, onFocusChange, onEnter)
    while running do
      draw(state, terminal)
      handleEvent(crosstermJni, state)

  private def init(
      root: File,
      focused: Option[File],
      onFocusChange: String,
      onEnter: String,
  ): AppState =
    val symbols = Config.unsafeLoad().symbols
    val files = Files(symbols)
    val git = Git(root, files)
    val state = AppState(root, files, git, symbols, onFocusChange, onEnter)
    focused.foreach(state.focus)
    state.refresh()
    state

  private def draw(appState: AppState, terminal: Terminal): Unit =
    val _ = terminal.draw(frame => render(frame, appState))

  private def render(frame: Frame, state: AppState): Unit =
    import AppState.Mode
    state.mode match
      case Mode.Tree => renderList(frame, state)
      case Mode.Help => renderHelp(frame)
      case Mode.CreateFile | Mode.CreateDir | Mode.Rename =>
        renderInput(frame, state)

  private def renderHelp(frame: Frame): Unit =
    val n = (s: String) => Span.nostyle(s)
    val b =
      (s: String) => Span.styled(s, Style.DEFAULT.addModifier(Modifier.BOLD))
    val help = ParagraphWidget(
      block = Some(BlockWidget(
        borders = Borders.ALL,
        title =
          Some(Spans.styled("Shortcuts", Style.DEFAULT.addModifier(Modifier.BOLD))),
        borderType = BlockWidget.BorderType.Rounded,
      )),
      text = Text.fromSpans(
        Spans.from(b("q"), n(": "), b("q"), n("uit\n")),
        Spans.from(b("r"), n(": "), b("r"), n("ename\n")),
        Spans.from(b("t"), n(": "), b("t"), n("ouch file\n")),
        Spans.from(b("m"), n(": "), b("m"), n("kdir\n")),
        Spans.from(b("i"), n(": "), n("show/hide "), b("i"), n("gnored\n")),
        Spans.from(b("d"), n(": "), b("d"), n("elete\n")),
        Spans.from(b("j, <Down>"), n(": "), n("focus next\n")),
        Spans.from(b("k, <Up>"), n(": "), n("focus prev\n")),
        Spans
          .from(b("<Return>, <Space>"), n(": "), n("toggle dir or open file\n")),
        Spans.from(b("<Esc>"), n(": "), n("run 'on-enter' callback\n")),
      ),
    )
    frame.renderWidget(help, frame.size)

  private def renderInput(frame: Frame, state: AppState): Unit =
    val chunks = Layout(
      direction = Direction.Vertical,
      margin = Margin(2),
      constraints = Array(Constraint.Length(1), Constraint.Length(1)),
    ).split(frame.size)

    val help = ParagraphWidget(
      text = Text.from(
        Span("<Esc>", Style.DEFAULT.addModifier(Modifier.BOLD)),
        Span.nostyle(" to cancel, "),
        Span("<Return>", Style.DEFAULT.addModifier(Modifier.BOLD)),
        Span.nostyle(" to finish"),
      ),
      alignment = Alignment.Center,
    )
    frame.renderWidget(help, chunks.head)
    val input = ParagraphWidget(
      text = Text.nostyle(state.userInput),
      block = Some(BlockWidget(
        borders = Borders.ALL,
        title = Some(Spans.styled(
          state.mode.toString(),
          Style.DEFAULT.addModifier(Modifier.BOLD),
        )),
        borderType = BlockWidget.BorderType.Rounded,
      )),
    )
    frame.renderWidget(input, chunks.last)
    frame.setCursor(
      x = chunks.last.x + Grapheme(state.userInput).width + 1,
      y = chunks.last.y + 1,
    )

  private def renderList(frame: Frame, state: AppState): Unit =
    val rect = frame.size
    val top = Rect(x = 0, y = 0, width = rect.width, height = rect.height - 1)
    val bottom = Rect(x = 0, y = rect.bottom - 1, width = rect.width, height = 1)

    val items = state.listWidgets
    val list = ListWidget(
      items = items,
      startCorner = Corner.TopRight,
      highlightStyle = Style.DEFAULT.addModifier(Modifier.BOLD),
    )
    val selected = state.listWidgetState

    frame.renderStatefulWidget(list, top)(selected)

    val help = ParagraphWidget(
      Text.from(Span("'?' for help", Style.DEFAULT.addModifier(Modifier.ITALIC)))
    )
    frame.renderWidget(help, bottom)

  private def handleEvent(jni: CrosstermJni, state: AppState) =
    import tui.crossterm.Event
    import tui.crossterm.KeyCode
    import AppState.Mode

    jni.read() match
      case key: Event.Key => state.mode match

          case Mode.Help => key.keyEvent().code match
              case q: KeyCode.Char if q.c() == 'q' => state.hideHelp()
              case _: KeyCode.Esc => state.hideHelp()
              case _ => ()

          case Mode.Tree => key.keyEvent().code match
              case q: KeyCode.Char if q.c() == 'q' => running = false

              case _: KeyCode.Esc => state.openFile()

              case r: KeyCode.Char if r.c() == 'r' => state.rename()
              case t: KeyCode.Char if t.c() == 't' => state.createFile()
              case m: KeyCode.Char if m.c() == 'm' => state.createDir()

              case i: KeyCode.Char if i.c() == 'i' => state.toggleIgnored()

              case d: KeyCode.Char if d.c() == 'd' => state.deleteFocused()

              case j: KeyCode.Char if j.c() == 'j' => state.focusNext()
              case k: KeyCode.Char if k.c() == 'k' => state.focusPrev()
              case _: KeyCode.Down => state.focusNext()
              case _: KeyCode.Up => state.focusPrev()

              case _: KeyCode.Enter => state.toggleFocusedOrOpen()

              case spc: KeyCode.Char if spc.c() == ' ' =>
                state.toggleFocusedOrOpen()

              case question: KeyCode.Char if question.c() == '?' =>
                state.showHelp()
              case _ => ()

          case Mode.CreateDir | Mode.CreateFile | Mode.Rename =>
            key.keyEvent().code match
              case _: KeyCode.Esc => state.cancelInput()

              case _: KeyCode.Enter => state.mode match
                  case Mode.CreateDir => state.createDir()
                  case Mode.CreateFile => state.createFile()
                  case Mode.Rename => state.rename()
                  case Mode.Help | Mode.Tree => sys.error("Unreachable")

              case _: KeyCode.Backspace =>
                state.userInput = state.userInput.init

              case c: KeyCode.Char => state.userInput = state.userInput + c.c()

              case _ => ()

      case _ => ()
