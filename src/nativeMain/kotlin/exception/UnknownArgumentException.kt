package exception

class UnknownArgumentException(override val message: String?) : BthException(message, 64)