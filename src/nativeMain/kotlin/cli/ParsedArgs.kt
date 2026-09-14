package cli

import cli.command.Command

class ParsedArgs(
    private val values: Map<String, Any?>,
    val positionals: List<String> = emptyList()
) {
    fun flag(spec: OptSpec.Flag): Boolean = values[spec.name] as? Boolean ?: false

    @Suppress("UNCHECKED_CAST")
    fun <T> value(spec: OptSpec.Value<T>): T = values[spec.name] as T

    @Suppress("UNCHECKED_CAST")
    fun subcommand(): Pair<Command, ParsedArgs>? = values["subcommand"] as? Pair<Command, ParsedArgs>
}
