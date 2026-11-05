package shared

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.TIOCGWINSZ
import platform.posix.ioctl
import platform.posix.winsize

@OptIn(markerClass = [ExperimentalForeignApi::class])
actual fun ioctl(ttyFd: Int, ws: CPointer<winsize>): Int = ioctl(ttyFd, TIOCGWINSZ, ws)