package cli.command

import cli.OptSpec
import cli.ParsedArgs
import db.withNSqlite

data object Precmd : Command { //--dir "$(pwd)" --elv "$elevated" --exc $EXIT --bhi "$BTH_HISTORY_ID"
    override val names = setOf("precmd")
    override val cmdHelp = "Records the results of a command."

    val dir = OptSpec.Value(name = "dir", convert = { it }, help = "The directory the command completed in. i.e. cd <somedir>, this would be <somedir> and not pwd before cd.")
    val elv = OptSpec.Value(name = "elv", convert = String::toInt, help = "The sudo elevation status. Used in tracking changes between valid sudo sessions available in the current shell.")
    val exc = OptSpec.Value(name = "exc", convert = String::toInt, help = "The exit code of the command.")
    val bhi = OptSpec.Value(name = "bhi", convert = String::toInt, help = "The id of the preexec cmd. Used for completing the history record.")
    val ert = OptSpec.Value(name = "ert", convert = { it }, help = $$"Epoch real time, should be from the $EPOCHREALTIME env var or date +%s.%N")
    override val spec = listOf(dir, elv, exc, bhi, ert)

    override fun execute(args: ParsedArgs) {
        withNSqlite { nSqlite ->
            nSqlite.history.updatePrecmd(
                args.value(ert),
                args.value(exc),
                args.value(bhi),
                args.value(elv)
            )
        }
    }
}
