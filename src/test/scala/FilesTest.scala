import java.nio.file.Files as JFiles
import java.nio.file.Paths
import munit.FunSuite

class FilesTest extends FunSuite:
  val files = Files(Config.default.symbols)

  test("delete + hierarchy down"):
      val root = Paths.get("./foo")
      val dir = root.resolve("bar").resolve("baz")
      val file = dir.resolve("file.ext")

      try files.deleteRecursively(root.toFile())
      catch _ => ()

      JFiles.createDirectories(dir)
      JFiles.createFile(file)
      val result = files.hierarchyDown(root.toFile())

      val expected =
        Set(root, root.resolve("bar"), root.resolve("bar").resolve("baz"), file)
          .map(path => path.toFile())
      assertEquals(result, expected)

      files.deleteRecursively(root.toFile())

  test("delete + hierarchy up"):
      val root = Paths.get("./foo")
      val dir = root.resolve("bar").resolve("baz")
      val file = dir.resolve("file.ext")

      try files.deleteRecursively(root.toFile())
      catch _ => ()

      JFiles.createDirectories(dir)
      JFiles.createFile(file)
      val result = files.hierarchyUp(file.toFile(), root.toFile())

      val expected =
        Set(root, root.resolve("bar"), root.resolve("bar").resolve("baz"), file)
          .map(path => path.toFile())
      assertEquals(result, expected)

      files.deleteRecursively(root.toFile())

  test("delete + build tree"):
      val A = Paths.get("./A")

      try files.deleteRecursively(A.toFile())
      catch _ => ()

      val B = A.resolve("B")
      val C = B.resolve("C")
      val D = B.resolve("D")
      val E = A.resolve("E")

      List(A, B, C, D, E).foreach(path => JFiles.createDirectories(path))

      val result = files
        .buildRecursiveTreeWithPrefixMarks(
          A.toFile(),
          show = _ => true,
          opened = _ => true,
        )
        .map((file, prefix) => prefix + file.getName())
        .mkString("\n")

      assertEquals(
        result,
        """|A
           |├─ B
           |│  ├─ C
           |│  ╰─ D
           |╰─ E""".stripMargin,
      )

      files.deleteRecursively(A.toFile())

  test("delete + hierarchy down while one child"):
      val A = Paths.get("./A")

      try files.deleteRecursively(A.toFile())
      catch _ => ()

      val B = A.resolve("B")
      val C = B.resolve("C")
      val fileC1 = C.resolve("file-c-1.ext")
      val fileC2 = C.resolve("file-c-2.ext")
      val D = C.resolve("D")
      val fileD = D.resolve("file-d.ext")

      List(A, B, C, D).foreach(path => JFiles.createDirectories(path))
      List(fileC1, fileC2, fileD).foreach(path => JFiles.createFile(path))

      val result = files.hieraryDownDirsWithOneChild(A.toFile())

      assertEquals(result, Set(A, B, C).map(path => path.toFile()))

      files.deleteRecursively(A.toFile())
