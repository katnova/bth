package cli

import exception.BthException

sealed class ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Failure(val exception: BthException) : ParseResult<Nothing>()
}
