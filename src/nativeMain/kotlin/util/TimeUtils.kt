package util

import kotlin.time.Duration
import kotlin.time.Instant

fun parseEpochRealtime(value: String): Instant {
    val parts = value.trim().split('.', limit = 2)

    val seconds = parts[0].toLong()

    val nanos = parts
        .getOrNull(1)
        ?.take(9)
        ?.padEnd(9, '0')
        ?.toLong()
        ?: 0L

    return Instant.fromEpochSeconds(seconds, nanos)
}

fun Duration.toPrettyString() = when {
    this.inWholeMicroseconds < 1000 -> "${this.inWholeMicroseconds}μs"
    this.inWholeMilliseconds < 1 -> "${(this.inWholeMicroseconds / 1000.0)}ms"
    this.inWholeSeconds < 1 -> "${this.inWholeMilliseconds}ms"
    this.inWholeMinutes < 1 -> "${this.inWholeSeconds}s"
    this.inWholeHours < 1 -> "${this.inWholeMinutes}m"
    this.inWholeDays < 1 -> "${this.inWholeHours}h"
    else -> "${this.inWholeDays}d"
}