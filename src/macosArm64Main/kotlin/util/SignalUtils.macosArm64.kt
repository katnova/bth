package util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.SIGWINCH
import platform.posix.SIG_BLOCK
import platform.posix.SIG_UNBLOCK
import platform.posix.pthread_sigmask
import platform.posix.sigaddset
import platform.posix.sigemptyset
import platform.posix.sigset_tVar


/**
 * Blocks [SIGWINCH] on the calling thread.
 *
 * [SIGWINCH] is blocked process-wide in `main()`, but any thread whose mask was temporarily
 * unblocked (see [unblockSigwinch]) must restore it.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun blockSigwinch() {
    memScoped {
        val set = alloc<sigset_tVar>()
        sigemptyset(set.ptr)
        sigaddset(set.ptr, SIGWINCH)
        pthread_sigmask(SIG_BLOCK, set.ptr, null)
    }
}

/**
 * Unblocks [SIGWINCH] on the calling (OS) thread.
 *
 * Used to hand an unblocked mask to threads created by C libraries: a new pthread inherits the
 * signal mask of the thread that created it, so `notcurses_init` must be called with [SIGWINCH]
 * unblocked for the internal input thread notcurses spawns (the one parked in `poll()` on the
 * tty) to ever be woken by a terminal resize. Re-block with [blockSigwinch] immediately after.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun unblockSigwinch() {
    memScoped {
        val set = alloc<sigset_tVar>()
        sigemptyset(set.ptr)
        sigaddset(set.ptr, SIGWINCH)
        pthread_sigmask(SIG_UNBLOCK, set.ptr, null)
    }
}