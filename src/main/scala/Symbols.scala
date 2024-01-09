import io.circe.Codec

final case class Symbols(
    dirClosed: String,
    dirOpened: String,
    extensions: Map[String, String],
    file: String,
    treeChild: String,
    treeEmpty: String,
    treeLastChild: String,
    treePipe: String,
)

object Symbols:

  given Codec[Symbols] = Codec.forProduct8(
    "dir-closed",
    "dir-opened",
    "extensions",
    "file",
    "tree-child",
    "tree-empty",
    "tree-last-child",
    "tree-pipe",
  )(apply)(symbols =>
    (
      symbols.dirClosed,
      symbols.dirOpened,
      symbols.extensions,
      symbols.file,
      symbols.treeChild,
      symbols.treeEmpty,
      symbols.treeLastChild,
      symbols.treePipe,
    )
  )
