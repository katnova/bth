package util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import notcurses.SIGWINCH
import notcurses.SIG_BLOCK
import notcurses.SIG_UNBLOCK
import notcurses.pthread_sigmask
import notcurses.sigaddset
import notcurses.sigemptyset
import notcurses.sigset_t

/**
 * Blocks [SIGWINCH] on the calling thread.
 *
 * [SIGWINCH] is blocked process-wide in `main()`, but any thread whose mask was temporarily
 * unblocked (see [unblockSigwinch]) must restore it.
 */
@OptIn(ExperimentalForeignApi::class)
fun blockSigwinch() = memScoped {
    val set = alloc<sigset_t>()
    sigemptyset(set.ptr)
    sigaddset(set.ptr, SIGWINCH)
    pthread_sigmask(SIG_BLOCK, set.ptr, null)
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
fun unblockSigwinch() = memScoped {
    val set = alloc<sigset_t>()
    sigemptyset(set.ptr)
    sigaddset(set.ptr, SIGWINCH)
    pthread_sigmask(SIG_UNBLOCK, set.ptr, null)
}
