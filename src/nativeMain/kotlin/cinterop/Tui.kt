package cinterop

import cnames.structs.ncplane
import controller.InputEventFlow
import db.StorageLayer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import model.ActivePlane
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
import model.state.Counts
import controller.GlobalController
import controller.HistoryController
import store.TuiStore
import util.blockSigwinch
import util.extensions.adjustedMax
import util.extensions.assertNegativeOrThrow
import util.unblockSigwinch
import view.HistoryKPlane
import view.InputKPlane
import view.KPlane
import view.ParentKPlane
import view.PlaneContext
import view.PlaneLayout
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
            val stdPlane: CPointer<ncplane>? = notcurses_stddim_yx(nc, rows.ptr, cols.ptr)
            try {
                val tuiDispatcher = newFixedThreadPoolContext(SettingOption.TUI_NUM_THREADS.read<Int>(), "BTH")
                val tuiJob = Job(coroutineContext.job)
                val tuiScope = CoroutineScope(coroutineContext + tuiDispatcher + tuiJob)
                var renderJob: Job? = null
                val store = TuiStore(
                    scope = tuiScope,
                    pwd = pwd,
                    inputBuffer = cmd,
                    initialRows = rows.value,
                    initialCols = cols.value,
                    initialCursorMax = (rows.adjustedMax() - 1).coerceAtLeast(0),
                    counts = Counts(0, 0, 0),
                )

                try {
                    val ctx = PlaneContext(
                        stdPlane = stdPlane,
                        scope = tuiScope,
                        drawChannel = store.drawChannel,
                        view = store.view
                    )

                    val parent = ParentKPlane(
                        ctx = ctx,
                        layout = PlaneLayout(
                            rows = { it },
                            cols = { it },
                            x = { 0 },
                            y = { 0 }
                        ),
                        history = store.history,
                        query = store.query,
                        counts = store.counts
                    )
                    val input = InputKPlane(
                        ctx = ctx,
                        layout = PlaneLayout(
                            rows = { 1u },
                            cols = { it - 2u },
                            x = { 0 },
                            y = { it - 2 }
                        ),
                        query = store.query
                    )
                    val history = HistoryKPlane(
                        ctx = ctx,
                        layout = PlaneLayout(
                            rows = { it - 3u },
                            cols = { it - 1u },
                            x = { 0 },
                            y = { 1 }
                        ),
                        history = store.history
                    )
                    val settings = SettingsKPlane(
                        ctx = ctx,
                        layout = PlaneLayout(
                            rows = { it - 2u },
                            cols = { it - 1u },
                            x = { 0 },
                            y = { 1 }
                        )
                    )

                    listOf(parent, input, history, settings).forEach { it.launch() }

                    HistoryController(
                        query = store.query,
                        view = store.view,
                        history = store.history,
                        pageSize = { rows.adjustedMax() }
                    ).launch(tuiScope)

                    // Which planes get first look at a key, in order.
                    val focus: Map<ActivePlane, List<KPlane<*>>> = mapOf(
                        ActivePlane.HISTORY_PLANE to listOf(history, input),
                        ActivePlane.SETTINGS_PLANE to listOf(settings),
                    )

                    val global = GlobalController(
                        store = store,
                        pwd = pwd,
                        tabOutputTempFile = ttmp,
                        enterOutputTempFile = etmp,
                        rowCPointer = rows,
                        colCPointer = cols,
                        nc = nc
                    )

                    renderJob = launch(newSingleThreadContext("BTH_RENDER")) {
                        blockSigwinch()

                        while (store.view.value.isRunning) {
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

                    inputEventFlow.flow().collect { action ->
                        val planes = focus[store.view.value.activePlane].orEmpty()
                        if (planes.none { it.onAction(action) }) global.handle(action)   //none stops at the first plane that handles it
                        if (!store.view.value.isRunning) this@coroutineScope.coroutineContext.job.cancelChildren()
                    }
                } catch (_: CancellationException) {
                } finally {
                    withContext(NonCancellable) {
                        store.drawChannel.close()
                        renderJob?.cancelAndJoin()
                        tuiJob.cancelAndJoin()
                    }
                    tuiDispatcher.close()
                }
            } finally {
                notcurses_stop(nc)
            }
        }
    }
}
