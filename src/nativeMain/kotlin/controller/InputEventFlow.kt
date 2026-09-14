package controller

import cnames.structs.notcurses
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.newSingleThreadContext
import model.KeyBind
import notcurses.CLOCK_MONOTONIC
import notcurses.NCKEY_BACKSPACE
import notcurses.NCKEY_DEL
import notcurses.NCKEY_DOWN
import notcurses.NCKEY_ENTER
import notcurses.NCKEY_ESC
import notcurses.NCKEY_HOME
import notcurses.NCKEY_LEFT
import notcurses.NCKEY_PGDOWN
import notcurses.NCKEY_PGUP
import notcurses.NCKEY_RESIZE
import notcurses.NCKEY_RIGHT
import notcurses.NCKEY_TAB
import notcurses.NCKEY_UP
import notcurses.clock_gettime
import notcurses.ncinput
import notcurses.ncinput_alt_p
import notcurses.ncinput_ctrl_p
import notcurses.ncinput_hyper_p
import notcurses.ncinput_meta_p
import notcurses.ncinput_shift_p
import notcurses.ncinput_super_p
import notcurses.ncintype_e
import notcurses.notcurses_get
import notcurses.timespec
import platform.posix.TIOCGWINSZ
import platform.posix.fileno
import platform.posix.ioctl
import platform.posix.stdout
import platform.posix.winsize
import setting.Settings
import store.TUIAction
import store.TuiStore
import util.blockSigwinch

@OptIn(ExperimentalForeignApi::class)
class InputEventFlow constructor(
    val nc: CPointer<notcurses>,
    val store: TuiStore
) {

    /**
     * Cannot distinguish between presses with `Shift` and other modifiers.
     *
     * > The Shift key is traditionally not indicated in conjunction with
     * > typical Unicode text. If e.g. Shift is used to generate a capital
     * > letter 'A', id will equal 'A', and shift will be false. Similarly,
     * > when Ctrl is pressed along with a letter, the letter will currently
     * > always be reported in its uppercase form. E.g., if Shift, Ctrl, and
     * > 'a' are all pressed, this is indistinguishable from Ctrl and 'A'.
     * - notcurses_input(3)
     */
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun flow(): Flow<TUIAction> = flow {
        memScoped {
            blockSigwinch()
            val input = alloc<ncinput>()
            val deadline = alloc<timespec>()
            val ttyFd = fileno(stdout)
            val ws = alloc<winsize>()
            val pollClock = alloc<timespec>()
            var nextPollMs = 0L

            var emittedRows = store.state.value.realRows
            var emittedCols = store.state.value.realCols

            emit(TUIAction.Init)
            while (store.state.value.isRunning) {
                deadline.deadlineIn(IDLE_WAIT_NS)
                val key = notcurses_get(nc, deadline.ptr, input.ptr)

                // 0 is a timeout, (uint32_t)-1 signals EOF/error
                if (key == 0u) {
                    // a resize is meant to arrive as NCKEY_RESIZE, but notcurses'
                    // internal poller is only woken by SIGWINCH, so any build/terminal that fails to
                    // deliver it leaves the resize sitting until the next real keystroke. Polling
                    // TIOCGWINSZ on idle makes resize handling independent of signal delivery.
                    val nowMs = pollClock.monotonicMs()
                    if (nowMs >= nextPollMs) {
                        nextPollMs = nowMs + RESIZE_POLL_INTERVAL_MS
                        if (ioctl(ttyFd, TIOCGWINSZ.toULong(), ws.ptr) == 0 && ws.ws_row > 0u && ws.ws_col > 0u) {
                            val rows = ws.ws_row.toUInt()
                            val cols = ws.ws_col.toUInt()
                            val current = store.state.value
                            val changed = rows != current.realRows || cols != current.realCols
                            if (changed && (rows != emittedRows || cols != emittedCols)) {
                                emittedRows = rows
                                emittedCols = cols
                                emit(TUIAction.Resize)
                            }
                        }
                    }
                    continue
                }
                if (key == UInt.MAX_VALUE) break

//                logInfo(
//                    "Key pressed: $key (raw: ${key.toInt()}), modifiers: ${input.modifiers}, evtype: ${input.evtype}, shift: ${input.shift}, ctrl: ${input.ctrl}, alt: ${input.alt}, meta: ${
//                        ncinput_meta_p(
//                            input.ptr
//                        )
//                    }, super: ${ncinput_super_p(input.ptr)}, hyper: ${ncinput_hyper_p(input.ptr)}"
//                )

                // Accept only keydown, some terminals support sending press/release, others don't
                if (key != 0u && input.evtype != ncintype_e.NCTYPE_RELEASE) {
                    val rawKey = key.toInt()

                    if(rawKey == NCKEY_RESIZE) {
                        emit(TUIAction.Resize)
                    }

                    // treat LF (10) and CR (13) as NCKEY_ENTER prior to lookup
                    val normalizedKey = if (rawKey == 10 || rawKey == 13) NCKEY_ENTER else rawKey


                    // logInfo("key: $rawKey, mod: ${input.modifiers}")

                    val mappedKeybindEvent = Settings.keybinds[KeyBind(
                        normalizedKey,
                        shift = ncinput_shift_p(input.ptr) || input.shift,
                        ctrl = ncinput_ctrl_p(input.ptr) || input.ctrl,
                        alt = ncinput_alt_p(input.ptr) || input.alt,
                        meta = ncinput_meta_p(input.ptr),
                        supr = ncinput_super_p(input.ptr),
                        hypr = ncinput_hyper_p(input.ptr)
                    )]
                    
                    if (mappedKeybindEvent != null) {
                        emit(mappedKeybindEvent)
                    } else if (isSpecial(normalizedKey)) {
                        // retry lookup ignoring modifiers for special keys
                        val relaxed = Settings.keybinds[KeyBind(
                            normalizedKey,
                            shift = false,
                            ctrl = false,
                            alt = false,
                            meta = false,
                            supr = false,
                            hypr = false
                        )]
                        if (relaxed != null) {
                            emit(relaxed)
                        } else if (rawKey in 32..0x10FF) {
                            emit(TUIAction.InputBufferAppend(rawKey.toChar()))
                        } else if (normalizedKey == NCKEY_ENTER) {
                            // if Enter still didn't map, execute selected
                            emit(TUIAction.HistoryExecuteSelected)
                        }
                    } else if (rawKey in 32..0x10FF) {
                        emit(TUIAction.InputBufferAppend(rawKey.toChar()))
                    } else if (normalizedKey == NCKEY_ENTER) {
                        // if Enter still didn't map, execute selected
                        emit(TUIAction.HistoryExecuteSelected)
                    }
                }
            }
        }
    }.flowOn(newSingleThreadContext("BTH_INPUT_FLOW"))

    private fun isSpecial(n: Int): Boolean = n in SPECIAL_NCKEYS

    /** Current CLOCK_MONOTONIC reading in milliseconds, using this [timespec] as scratch space. */
    private fun timespec.monotonicMs(): Long {
        clock_gettime(CLOCK_MONOTONIC, this.ptr)
        return this.tv_sec * MILLIS_PER_SEC + this.tv_nsec / NANOS_PER_MILLI
    }

    /**
     * Sets this [timespec] to an absolute CLOCK_MONOTONIC deadline [nanos] from now,
     * as expected by `notcurses_get`.
     */
    private fun timespec.deadlineIn(nanos: Long) {
        clock_gettime(CLOCK_MONOTONIC, this.ptr)
        var sec = this.tv_sec
        var nsec = this.tv_nsec + nanos
        if (nsec >= NANOS_PER_SEC) {
            sec += (nsec / NANOS_PER_SEC)
            nsec %= NANOS_PER_SEC
        }
        this.tv_sec = sec
        this.tv_nsec = nsec
    }

    companion object {
        private const val NANOS_PER_SEC = 1_000_000_000L
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val MILLIS_PER_SEC = 1_000L

        /** How long a single read waits before the loop re-checks [store] state. */
        private const val IDLE_WAIT_NS = 5_000_000L

        /** How often the idle tick asks the kernel for the terminal size. */
        private const val RESIZE_POLL_INTERVAL_MS = 32L

        private val SPECIAL_NCKEYS = setOf(
            NCKEY_ENTER, NCKEY_ESC, NCKEY_TAB, NCKEY_UP, NCKEY_DOWN, NCKEY_LEFT,
            NCKEY_RIGHT, NCKEY_PGUP, NCKEY_PGDOWN, NCKEY_HOME, NCKEY_DEL, NCKEY_BACKSPACE
        )
    }
}
