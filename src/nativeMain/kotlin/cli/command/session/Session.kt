package cli.command

import cli.OptSpec
import cli.ParsedArgs
import db.NativeSqliteService

data object Session : Command {
    override val names = setOf("session")
    override val cmdHelp = "Generates a session ID."

    val ert = OptSpec.Value(name = "ert", convert = { it }, help = $$"Epoch real time, should be from the $EPOCHREALTIME env var or date +%s.%N")
    override val spec = listOf(ert)

    override fun execute(args: ParsedArgs) {
        print(NativeSqliteService().sessions.create(args.value(ert)))
    }
}
