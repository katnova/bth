package exception

class MissingRequiredArgException(override val message: String?) : BthException(message, 64)