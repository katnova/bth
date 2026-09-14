package util

inline fun <reified T : Enum<T>> Enum<T>.next(): T {
    return if(this.ordinal < (enumValues<T>().size - 1))
        enumValues<T>()[this.ordinal + 1]
    else enumValues<T>()[this.ordinal]
}

inline fun <reified T : Enum<T>> Enum<T>.previous(): T {
    return if(this.ordinal > 0)
        enumValues<T>()[this.ordinal - 1]
    else enumValues<T>()[this.ordinal]
}

/**
 * Inline fun to loop though an Enum using ordinals.
 */
inline fun <reified T : Enum<T>> Enum<T>.nextLooping(): T {
    return if(this.ordinal < (enumValues<T>().size - 1))
        enumValues<T>()[this.ordinal + 1]
    else enumValues<T>().first()
}
