package cli.command

import buildinfo.BuildInfo
import cli.OptSpec
import cli.ParsedArgs

data object Help : Command {
    override val names = setOf("help")
    override val cmdHelp = "Outputs general help information"
    override val spec = emptyList<OptSpec<*>>()

    override fun execute(args: ParsedArgs) {
        var maxPad = 2
        for (entry in Command.entries) {
            var padding = 0
            entry.names.forEach { padding += (it.length + 2) } //account for .toString() ", " and "["/"]"
            if (maxPad < padding) maxPad = padding
        }
        println(
            """
bth v${BuildInfo.VERSION}
Better Terminal History

Usage:
  bth <COMMAND> [OPTIONS]

For more help for a specific command, add `--help` after it. Example:
  bth <COMMAND> --help

Commands:
${Command.entries.joinToString("\n") { "  ${it.prettyNames().padEnd(maxPad)} - ${it.cmdHelp}" }}
        """.trimIndent()
        )
    }
}
