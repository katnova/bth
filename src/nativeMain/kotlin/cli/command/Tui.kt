package cli.command

import cli.OptSpec
import cli.ParsedArgs
import cinterop.Tui as NcTui
import kotlinx.coroutines.runBlocking

data object Tui : Command {
    override val names = setOf("tui")
    override val cmdHelp = "Invokes the TUI. Not intended to be invoked manually outside of development/testing."

    //todo: Combine ttmp & etmp
    val ttmp = OptSpec.Value(name = "ttmp", convert = { it }, help = "Absolute path to temp file used for outputting a selected command.")
    val etmp = OptSpec.Value(name = "etmp", convert = { it }, help = "Absolute path to temp file used for outputting and executing a selected command.")
    val pwd = OptSpec.Value(name = "pwd", convert = { it }, help = "Absolute path to pwd used for filtering history by dir")
    val tty = OptSpec.Value(name = "tty", isRequired = false, convert = { it }, help = "Absolute path to TTY/PTY to use. Only useful in special cases.")
    val cmd = OptSpec.Value(name = "cmd", isRequired = false, isNullable = true, convert = { it }, help = "The current command buffer, used as the initial text filter.")
    override val spec = listOf(ttmp, etmp, pwd, tty, cmd)

    override fun execute(args: ParsedArgs) {
        runBlocking {
            NcTui(
                ttmp = args.value(ttmp),
                etmp = args.value(etmp),
                tty = args.value(tty),
                pwd = args.value(pwd),
                cmd = args.value(cmd)
            ).run()
        }
    }
}
