lazy val root = project
  .in(file("."))
  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(
    name                := "kmtm",
    organization        := "tech.igorramazanov",
    maintainer          := "igor.ramazanov@protonmail.com",
    scalaVersion        := "3.3.1",
    scalacOptions       := List(
      "-Wnonunit-statement",
      "-Wunused:all",
      "-Wvalue-discard",
      "-Yno-experimental",
      "-Ysafe-init",
      "-deprecation",
      "-feature",
      "-language:fewerBraces",
      "-new-syntax",
      "-unchecked",
    ),
    libraryDependencies := List(
      "com.olvind.tui"        %% "tui"       % "0.0.7",
      "com.github.arturopala" %% "gitignore" % "0.6.0",
      "com.monovore"          %% "decline"   % "2.4.1",
      "org.scalameta"         %% "munit"     % "0.7.29" % Test,
    ),
    graalVMNativeImageOptions ++= List(
      "--verbose",
      "--no-fallback",
      "-H:+ReportExceptionStackTraces",
      "--initialize-at-build-time=scala.runtime.Statics$VM",
      "--initialize-at-build-time=scala.Symbol",
      "--initialize-at-build-time=scala.Symbol$",
      "--native-image-info",
      """-H:IncludeResources=libnative-arm64-darwin-crossterm.dylib""",
      """-H:IncludeResources=libnative-x86_64-darwin-crossterm.dylib""",
      """-H:IncludeResources=libnative-x86_64-linux-crossterm.so""",
      """-H:IncludeResources=native-x86_64-windows-crossterm.dll""",
      "-H:-UseServiceLoaderFeature",
      "-march=native",
      "--strict-image-heap",
    ),
  )
