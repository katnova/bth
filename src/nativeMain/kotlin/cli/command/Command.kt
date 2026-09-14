package cli.command

import cli.OptSpec
import cli.ParsedArgs

sealed interface Command {
    val names: Set<String>
    val cmdHelp: String
    val spec: List<OptSpec<*>>

    val subcommands: List<Command>
        get() = spec.filterIsInstance<OptSpec.Subcommands>().flatMap { it.commands }

    fun execute(args: ParsedArgs)

    fun prettyNames(): String = names.joinToString(", ")

    /** Auto-generated from [spec], so `--help` output can never drift from what's actually accepted. */
    fun usage(): String {
        val hasSubcommands = subcommands.isNotEmpty()
        val usageLine = "bth ${prettyNames()} [OPTIONS]${if (hasSubcommands) " COMMAND" else ""}"

        val commandsSection = if (hasSubcommands) {
            "\nCommands:\n" + subcommands.joinToString("\n") {
                "  ${it.names.first().padEnd(10)} - ${it.cmdHelp}"
            } + "\n"
        } else ""

        val optionsSection = spec.filter { it !is OptSpec.Subcommands }.joinToString("\n") {
            when (it) {
                is OptSpec.Flag -> "  --${it.name} - ${it.help}"
                is OptSpec.Value<*> -> {
                    val req = if (it.isRequired) '*' else ' '
                    "  $req --${it.name} <value> - ${it.help}"
                }
                is OptSpec.Subcommands -> "" // Filtered out, but for exhaustiveness
            }
        }

        return """
$cmdHelp

Usage: 
  $usageLine
$commandsSection
Options:
$optionsSection

* - Indicates required arg
    """.trimIndent()
    }

    companion object {
        val entries: List<Command> = listOf(Version, Preexec, Precmd, Shell, Tui, Config, Session, Bookmark, Help)
    }
}
