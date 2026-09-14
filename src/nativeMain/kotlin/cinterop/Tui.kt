package cinterop

import controller.InputEventFlow
import db.StorageLayer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import notcurses.NCOPTION_SCROLLING
import notcurses.NCOPTION_SUPPRESS_BANNERS
import notcurses.notcurses_init
import notcurses.notcurses_options
import notcurses.notcurses_render
import notcurses.notcurses_stddim_yx
import notcurses.notcurses_stop
import platform.posix.LC_ALL
import platform.posix.freopen
import platform.posix.setlocale
import platform.posix.stderr
import platform.posix.stdin
import platform.posix.stdout
import setting.SettingOption
import setting.Settings.read
import store.TuiStore
import util.blockSigwinch
import util.extensions.assertNegativeOrThrow
import util.unblockSigwinch
import view.HistoryKPlane
import view.InputKPlane
import view.ParentKPlane
import view.SettingsKPlane

@OptIn(ExperimentalForeignApi::class)
class Tui {
    val ttmp: String
    val etmp: String
    val tty: String?
    val pwd: String
    val cmd: String

    constructor(ttmp: String, etmp: String, tty: String? = null, pwd: String, cmd: String?) {
        setlocale(LC_ALL, "")
        this.ttmp = ttmp
        this.etmp = etmp
        this.tty = tty
        this.pwd = pwd
        this.cmd = cmd ?: ""
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    suspend fun run() = memScoped {
        coroutineScope {
            StorageLayer.load()

            if (tty != null) {
                freopen(tty, "r+", stdin)
                freopen(tty, "w+", stdout)
                freopen(tty, "w+", stderr)
            }

            /**
             * I did try NCOPTION_CLI_MODE. I may revisit when I figure out resize.
             *
             * It's easier to just utilize the default nc smcup/rmcup behavior atm.
             * Not sure if banking on smcup/rmcup could pose a compat issue.
             */
            val opts = alloc<notcurses_options>().apply {
                flags = NCOPTION_SCROLLING or NCOPTION_SUPPRESS_BANNERS
            }

            // notcurses spawns its own internal input thread inside notcurses_init, and a fresh
            // pthread inherits the signal mask of whichever thread created it.
            unblockSigwinch()
            val nc = try {
                notcurses_init(opts.ptr, stdout?.reinterpret())
                    ?: throw IllegalStateException("Failed to init notcurses! ;-;. TTY: $tty")
            } finally {
                blockSigwinch()
            }

            val rows = alloc<UIntVar>()
            val cols = alloc<UIntVar>()
            val stdPlane = notcurses_stddim_yx(nc, rows.ptr, cols.ptr)
            try {
                val tuiDispatcher = newFixedThreadPoolContext(SettingOption.TUI_NUM_THREADS.read<Int>(), "BTH")

                try {

                    val tuiScope = CoroutineScope(coroutineContext + tuiDispatcher)
                    val store = TuiStore(
                        rowCPointer = rows,
                        colCPointer = cols,
                        scope = tuiScope,
                        tabOutputTempFile = ttmp,
                        enterOutputTempFile = etmp,
                        pwd = pwd,
                        inputBuffer = cmd,
                        notcursesCPointer = nc
                    )

                    launch(tuiDispatcher) {
                        blockSigwinch()

                        ParentKPlane(
                            stdPlane = stdPlane,
                            tuiStore = store,
                            computeUiRows = { it },
                            computeUiCols = { it },
                            computeXCord = { 0 },
                            computeYCord = { 0 }
                        ).launch()

                        InputKPlane(
                            stdPlane = stdPlane,
                            tuiStore = store,
                            computeUiRows = { 1u },
                            computeUiCols = { it - 2u },
                            computeXCord = { 0 },
                            computeYCord = { it - 2 }
                        ).launch()

                        HistoryKPlane(
                            stdPlane = stdPlane,
                            tuiStore = store,
                            computeUiRows = { it - 3u },
                            computeUiCols = { it - 1u },
                            computeXCord = { 0 },
                            computeYCord = { 1 }
                        ).launch()

                        SettingsKPlane(
                            stdPlane = stdPlane,
                            tuiStore = store,
                            computeUiRows = { it - 2u },
                            computeUiCols = { it - 1u },
                            computeXCord = { 0 },
                            computeYCord = { 1 }
                        ).launch()
                    }

                    /**
                     * Render pipeline:
                     *
                     * [keypress] -> [store]  -> [HistoryKPlane] \
                     *                        -> [InputKPlane]    >-> [drawChannel] -> [render actor] -> [render]
                     *                        -> [ParentKPlane]  /
                     */

                    launch(newSingleThreadContext("BTH_RENDER")) {
                        blockSigwinch()

                        while (store.state.value.isRunning) {
                            val first = store.drawChannel.receiveCatching().getOrNull() ?: break
                            var batched = 1
                            first()
                            while (true) {
                                val next = store.drawChannel.tryReceive().getOrNull() ?: break
                                next()
                                batched++
                            }
                            notcurses_render(nc).assertNegativeOrThrow("Failed to render!")
                        }
                    }

                    val inputEventFlow = InputEventFlow(nc = nc, store = store)

                    try {
                        inputEventFlow.flow()
                            .conflate()
                            .collect {
                                store.handleEvent(it)
                                if (!store.state.value.isRunning) {
                                    this@coroutineScope.coroutineContext.job.cancelChildren()
                                }
                            }
                    } catch (_: CancellationException) {
                        // Expected on exit
                    }
                } finally {
                    tuiDispatcher.close()
                }
            } finally {
                notcurses_stop(nc)
            }
        }
    }
}
