package util.extensions

fun Int.assertNegativeOrThrow(msg: String) {
    if (this < 0) throw IllegalStateException(msg)
}