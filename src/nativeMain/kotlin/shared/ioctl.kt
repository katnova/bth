package shared

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.winsize

@OptIn(ExperimentalForeignApi::class)
expect fun ioctl(ttyFd: Int, ws: CPointer<winsize>): Int