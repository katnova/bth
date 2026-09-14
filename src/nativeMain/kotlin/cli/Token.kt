package cli

sealed interface Token {
    data class LongOpt(val opt: String, val value: String) : Token
    data class ShortOpt(val opt: Char) : Token
    data class EndOfOpts(val marker: String) : Token
    data class Positional(val value: String) : Token
    data class Unknown(val value: String) : Token
}