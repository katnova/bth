package cli

import exception.MissingRequiredArgException
import exception.UnknownArgumentException

object Binder {
    fun bind(tokens: List<Token>, specs: List<OptSpec<*>>): ParseResult<ParsedArgs> {
        val byName = specs.associateBy { it.name }
        val byShort = specs.filter { it.short != null }.associateBy { it.short }
        val values = mutableMapOf<String, Any?>()
        val positionals = mutableListOf<String>()
        val subcommandsSpec = specs.filterIsInstance<OptSpec.Subcommands>().firstOrNull()

        for ((index, token) in tokens.withIndex()) {
            when (token) {
                is Token.LongOpt -> {
                    val spec = byName[token.opt]
                        ?: return ParseResult.Failure(UnknownArgumentException("Unknown argument: --${token.opt}"))

                    when (spec) {
                        is OptSpec.Flag -> values[spec.name] = true
                        is OptSpec.Subcommands -> {}
                        is OptSpec.Value<*> -> {
                            if (token.value.isEmpty() && !spec.isNullable) {
                                return ParseResult.Failure(
                                    MissingRequiredArgException("Missing value for --${spec.name}. Tokens: $tokens")
                                )
                            }
                            values[spec.name] = try {
                                spec.convert(token.value)
                            } catch (_: Exception) {
                                //todo: Typed error messages, rather than catch-all
                                return ParseResult.Failure(
                                    UnknownArgumentException("Invalid value for --${spec.name}: ${token.value}")
                                )
                            }
                        }
                    }
                }

                is Token.ShortOpt -> {
                    val spec = byShort[token.opt]
                        ?: return ParseResult.Failure(UnknownArgumentException("Unknown argument: -${token.opt}"))

                    when (spec) {
                        is OptSpec.Flag -> values[spec.name] = true
                        is OptSpec.Subcommands -> {}
                        is OptSpec.Value<*> -> return ParseResult.Failure(
                            UnknownArgumentException("-${token.opt} requires a value, which short options don't support")
                        )
                    }
                }

                is Token.Positional -> positionals.add(token.value)
                is Token.Unknown -> {
                    if (subcommandsSpec != null) {
                        val subCommand = subcommandsSpec.commands.find { it.names.contains(token.value) }
                        if (subCommand != null) {
                            val remainingTokens = tokens.subList(index + 1, tokens.size)
                            return when (val subResult = bind(remainingTokens, subCommand.spec)) {
                                is ParseResult.Success -> {
                                    values[subcommandsSpec.name] = Pair(subCommand, subResult.value)
                                    finishBinding(specs, values, positionals)
                                }
                                is ParseResult.Failure -> subResult
                            }
                        }
                    }
                    return ParseResult.Failure(UnknownArgumentException("Unknown argument: ${token.value}"))
                }
                is Token.EndOfOpts -> Unit
            }
        }

        return finishBinding(specs, values, positionals)
    }

    private fun finishBinding(
        specs: List<OptSpec<*>>,
        values: MutableMap<String, Any?>,
        positionals: List<String>
    ): ParseResult<ParsedArgs> {
        for (spec in specs) {
            if (spec.name in values) continue
            when (spec) {
                is OptSpec.Flag -> values[spec.name] = false
                is OptSpec.Subcommands -> {
                    if (spec.isRequired) {
                        return ParseResult.Failure(MissingRequiredArgException("Missing required subcommand"))
                    }
                }
                is OptSpec.Value<*> -> when {
                    spec.default != null -> values[spec.name] = spec.default
                    spec.isRequired -> return ParseResult.Failure(
                        MissingRequiredArgException("Missing required argument: --${spec.name}")
                    )
                }
            }
        }

        return ParseResult.Success(ParsedArgs(values, positionals))
    }
}
