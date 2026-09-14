package cli.command

import cli.OptSpec
import cli.ParsedArgs
import db.withNSqlite

data object Preexec : Command {
    override val names = setOf("preexec")
    override val cmdHelp = "Records a command invocation."

    val dir = OptSpec.Value(name = "dir", convert = { it }, help = "The pwd the command was executed in.")
    val elv = OptSpec.Value(name = "elv", convert = { it }, help = "The sudo elevation status. Used in tracking changes between valid sudo sessions available in the current shell.")
    val ert = OptSpec.Value(name = "ert", convert = { it }, help = $$"Epoch real time, should be from the $EPOCHREALTIME env var or date +%s.%N")
    val ses = OptSpec.Value(name = "ses", convert = { it }, help = "The BTH session id. Should be available from env var BTH_SESSION_ID")
    val cmd = OptSpec.Value(name = "cmd", convert = { it }, help = "The command that was executed.")
    override val spec = listOf(dir, elv, ert, ses, cmd)

    override fun execute(args: ParsedArgs) {
        withNSqlite { nSqlite -> //todo: Needs to be null-safe
            println(
                nSqlite.history.insertPreexec(
                    now = args.value(ert),
                    dir = args.value(dir),
                    elevated = args.value(elv),
                    cmd = args.value(cmd),
                    session = args.value(ses)
                )
            )
        }
    }
}
