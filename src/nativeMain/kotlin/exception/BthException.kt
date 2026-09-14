package exception

/**
 * Reference sysexits.h for codes.
 */
open class BthException(override val message: String?, val exitCode: Int = 1): Exception(message)