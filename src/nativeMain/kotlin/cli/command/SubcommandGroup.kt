package cli.command

import cli.OptSpec
import cli.ParsedArgs

abstract class SubcommandGroup(
    subcommands: List<Command>,
    isRequired: Boolean = true
) : Command {
    private val subcommandSpec = OptSpec.Subcommands(subcommands, isRequired)
    override val spec: List<OptSpec<*>> get() = listOf(subcommandSpec)
    override fun execute(args: ParsedArgs) {
        val (cmd, subArgs) = args.subcommand() ?: return
        cmd.execute(subArgs)
    }
}