package shared

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.TIOCGWINSZ
import platform.posix.ioctl
import platform.posix.winsize

@OptIn(ExperimentalForeignApi::class)
actual fun ioctl(ttyFd: Int, ws: CPointer<winsize>) = ioctl(ttyFd, TIOCGWINSZ.toULong(), ws)