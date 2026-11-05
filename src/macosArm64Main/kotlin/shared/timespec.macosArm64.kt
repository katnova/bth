package shared

import controller.InputEventFlow.Companion.MILLIS_PER_SEC
import controller.InputEventFlow.Companion.NANOS_PER_MILLI
import controller.InputEventFlow.Companion.NANOS_PER_SEC
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

/** Current CLOCK_MONOTONIC reading in milliseconds, using this [timespec] as scratch space. */
@OptIn(ExperimentalForeignApi::class)
actual fun timespec.monotonicMs(): Long {
    clock_gettime(CLOCK_MONOTONIC.toUInt(), this.ptr)
    return this.tv_sec * MILLIS_PER_SEC + this.tv_nsec / NANOS_PER_MILLI
}

/**
 * Sets this [timespec] to an absolute CLOCK_MONOTONIC deadline [nanos] from now,
 * as expected by `notcurses_get`.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun timespec.deadlineIn(nanos: Long) {
    clock_gettime(CLOCK_MONOTONIC.toUInt(), this.ptr)
    var sec = this.tv_sec
    var nsec = this.tv_nsec + nanos
    if (nsec >= NANOS_PER_SEC) {
        sec += (nsec / NANOS_PER_SEC)
        nsec %= NANOS_PER_SEC
    }
    this.tv_sec = sec
    this.tv_nsec = nsec
}