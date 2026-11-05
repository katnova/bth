package shared

import platform.posix.timespec

expect fun timespec.monotonicMs(): Long

expect fun timespec.deadlineIn(nanos: Long)