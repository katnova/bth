package cli


import cli.command.Command

sealed class OptSpec<T>(
    val name: String,
    val short: Char? = null,
    val help: String? = ""
) {
    /** A presence-only switch, e.g. `--elv`. Never consumes a value. */
    class Flag(
        name: String,
        short: Char? = null,
        help: String? = ""
    ) : OptSpec<Boolean>(name, short, help)

    /** An option that takes a value and converts it to [T], e.g. `--exc 12`. */
    class Value<T>(
        name: String,
        short: Char? = null,
        val isRequired: Boolean = true,
        val isNullable: Boolean = false,
        val default: T? = null,
        val convert: (String) -> T,
        help: String? = ""
    ) : OptSpec<T>(name, short, help)

    class Subcommands(
        val commands: List<Command>,
        val isRequired: Boolean = true
    ) : OptSpec<Pair<Command, ParsedArgs>>("subcommand", help = "The subcommand to execute")
}