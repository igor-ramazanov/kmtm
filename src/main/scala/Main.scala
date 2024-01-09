import com.monovore.decline.CommandApp

object Main
    extends CommandApp(name = Cli.name, header = Cli.header, main = Cli.run)
