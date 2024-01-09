import io.circe.syntax.*
import io.circe.Codec
import java.nio.file.Files
import java.nio.file.Paths
import scala.util.chaining.*

final case class Config(symbols: Symbols)

object Config:

  given Codec[Config] = Codec
    .forProduct1("symbols")(apply)(config => config.symbols)

  val default = Config(symbols =
    Symbols(
      dirClosed = " ",
      dirOpened = " ",
      file = " ",
      treeChild = "├─ ",
      treeEmpty = "  ",
      treeLastChild = "╰─ ",
      treePipe = "│  ",
      extensions = Map(
        "bash" -> " ",
        "clj" -> " ",
        "cljc" -> " ",
        "cljr" -> " ",
        "cljs" -> " ",
        "cql" -> " ",
        "edn" -> " ",
        "go" -> " ",
        "hs" -> " ",
        "html" -> " ",
        "jar" -> " ",
        "java" -> " ",
        "jpeg" -> " ",
        "jpg" -> " ",
        "js" -> " ",
        "json" -> " ",
        "md" -> " ",
        "nix" -> " ",
        "nu" -> " ",
        "php" -> " ",
        "png" -> " ",
        "py" -> " ",
        "rb" -> " ",
        "rs" -> " ",
        "sbt" -> " ",
        "sc" -> " ",
        "scala" -> " ",
        "sh" -> " ",
        "sql" -> " ",
        "ts" -> " ",
        "zsh" -> " ",
      ),
    ),
  )

  def unsafeLoad(): Config = Paths
    .get(
      sys
        .env
        .getOrElse(
          "XDG_CONFIG_HOME",
          Paths
            .get(sys.env("HOME"))
            .resolve(".config")
            .toAbsolutePath()
            .toString(),
        )
    )
    .resolve("kmtm")
    .tap(Files.createDirectories(_))
    .pipe: path =>
      path.resolve("config.json")
    .tap: path =>
      if !path.toFile().exists() then
        val _ = Files.writeString(path, default.asJson.spaces2SortKeys)
    .pipe(Files.readString)
    .pipe(io.circe.parser.decode[Config])
    .pipe {
      case Left(err) => throw err
      case Right(config) => config
    }
