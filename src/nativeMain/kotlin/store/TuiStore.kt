package store

import cnames.structs.notcurses
import db.StorageLayer.nSqlite
import kotlin.math.abs
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.DbFetchFrame
import model.FilterModes
import model.Flag
import notcurses.notcurses_refresh
import util.extensions.adjustedMax
import util.next
import util.nextLooping
import util.previous
import util.writeFileLines

class TuiStore @OptIn(ExperimentalForeignApi::class) constructor(
    val scope: CoroutineScope,
    val tabOutputTempFile: String,
    val enterOutputTempFile: String,
    val pwd: String,
    val inputBuffer: String,
    val rowCPointer: UIntVar,
    val colCPointer: UIntVar,
    val notcursesCPointer: CPointer<notcurses>
) {
    private val _state = MutableStateFlow(TUIState(cwd = pwd, inputBuffer = inputBuffer))
    val state: StateFlow<TUIState> = _state.asStateFlow()

    val drawChannel: Channel<suspend () -> Unit> = Channel(capacity = Channel.BUFFERED)

    init {
        _state.value = size()
        // ensure cursor position is max on init
        _state.value = _state.value.copy(
            cursor = _state.value.cursorMax
        )

        val (totalCommands, totalDirs, totalHistory) = nSqlite.tableCounts()
        _state.value = _state.value.copy(
            totalCmds = totalCommands,
            totalDirs = totalDirs,
            totalHists = totalHistory
        )

        observeInputForDbFetch()
    }

    /**
     * Computes the current size of the terminal UI state by adjusting its rows and columns
     * based on the [rowCPointer] & [colCPointer] pointers and updates the state accordingly.
     *
     * @return A new instance of [TUIState] with updated values for cursor bounds, rows, columns,
     *         and real dimensions, and with the cursor position properly set.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun size(): TUIState {
        val adjustedRows = rowCPointer.adjustedMax()

        return _state.value.copy(
            cursorMax = (adjustedRows - 1),
            realRows = rowCPointer.value,
            realCols = colCPointer.value
        ).moveCursor(0)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun observeInputForDbFetch() { //This really needs to be in it's own class
        scope.launch {
            _state.takeWhile { it.isRunning }
                .map {
                    val sessionFilterId = if (it.flags.contains(Flag.SES_MODE))
                        it.renderedHistory.getOrNull(it.cursor)?.session ?: -1
                    else -1
                    DbFetchFrame(
                        buffer = it.inputBuffer,
                        offset = it.offset,
                        filterMode = it.filterMode,
                        cwd = it.cwd,
                        flags = it.flags,
                        sessionFilterId = sessionFilterId,
                        uiRows = it.realRows,
                        uiCols = it.realCols
                    )
                }
                .distinctUntilChanged()
                .collect {
                    val results = withContext(Dispatchers.Default) {
                        return@withContext when (it.filterMode) {
                            FilterModes.FM_DIR_ALL -> nSqlite.history.filter(
                                filter = it.buffer,
                                num = rowCPointer.adjustedMax(),
                                offset = it.offset,
                                it.sessionFilterId
                            )

                            FilterModes.FM_DIR_PWD -> nSqlite.history.filterPwd(
                                filter = it.buffer,
                                num = rowCPointer.adjustedMax(),
                                offset = it.offset,
                                pwd = it.cwd,
                                it.sessionFilterId
                            )

                            FilterModes.FM_DIR_SUB -> nSqlite.history.filterPwdSub(
                                filter = it.buffer,
                                num = rowCPointer.adjustedMax(),
                                offset = it.offset,
                                pwd = it.cwd,
                                it.sessionFilterId
                            )
                        }
                    }

                    _state.update { current ->
                        val size = results.size.coerceAtLeast(0)
                        val newMax = size.minus(1).coerceAtLeast(0)
                        current.copy(
                            renderedHistory = results,
                            cursorMax = newMax,
                            cursor = if (current.cursorMax != newMax) {
                                size.minus(1).coerceAtLeast(0)
                            } else current.cursor,
                            offset = if (size < rowCPointer.adjustedMax()) {
                                // This is a hack. I think this speaks to a deeper issues around flow controls.
                                // I'll probably need to build some sort of coordination to ensure OOO works as expected.
                                it.offset.minus(rowCPointer.adjustedMax() - size).coerceAtLeast(0)
                            } else it.offset
                        )
                    }
                }

        }
    }

    /**
     * Helper function to calculate new valid cursor positions.
     *
     * Provided a [potential] (i.e. +1, -1, +5, -5), determine the next valid position and update [cursor] in [TUIState].
     */
    private fun TUIState.moveCursor(potential: Int): TUIState {
        val result = (this.cursor + potential)
        var nextOffset = this.offset
        if (result > cursorMax && nextOffset > 0 && renderedHistory.isNotEmpty()
        ) {
            if(renderedHistory.size.minus(1) == cursorMax) {
                nextOffset -= (result - cursorMax).coerceAtLeast(0)
            }
        } else if (result < 0) {
            nextOffset += abs(result)
        }
        return this.copy(
            cursor = result.coerceIn(0, this.cursorMax),
            offset = nextOffset
        )
    }

    /**
     * Helper function to toggle flags in [TUIState].
     *
     * Allows specifying an [onOnBlock] and [onOffBlock] for side-effects.
     */
    private fun TUIState.toggleFlag(
        flag: Flag,
        onOnBlock: (TUIState) -> TUIState = { it },
        onOffBlock: (TUIState) -> TUIState = { it }
    ): TUIState {
        return if (this.flags.contains(flag)) {
            onOffBlock(
                this.copy(flags = this.flags.filterNot { it == flag }.toTypedArray())
            )
        } else {
            onOnBlock(
                this.copy(flags = this.flags.plus(flag))
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun handleEvent(event: TUIAction) {
        val current = _state.value
        val next = when (event) {
            is TUIAction.HistoryScrollUp -> current.moveCursor(-1)
            is TUIAction.HistoryScrollDown -> current.moveCursor(1)
            is TUIAction.HistoryPageUp -> current.moveCursor(-5)
            is TUIAction.HistoryPageDown -> current.moveCursor(5)
            is TUIAction.InputBufferAppend -> current.copy(
                inputBuffer = current.inputBuffer + event.char,
                offset = 0
            )

            is TUIAction.InputBufferBackspace -> current.copy(
                inputBuffer = if (current.inputBuffer.isNotEmpty()) current.inputBuffer.dropLast(1) else "",
                offset = 0
            )

            is TUIAction.InputBufferClear -> current.copy(
                inputBuffer = "",
                offset = 0
            )

            is TUIAction.HistoryPasteSelected, is TUIAction.HistoryExecuteSelected -> {
                var cmdToRun = ""
                val record = current.renderedHistory[current.cursor]

                if (current.flags.contains(Flag.MTD_FLAG) || current.flags.contains(Flag.CP_MODE)) {
                    cmdToRun += "cd ${current.cwd} && "
                }

                //todo: Fragile, could break if a cmd did not start with sudo. i.e. a bash script containing sudo
                if (current.flags.contains(Flag.SU_MODE) || (current.flags.contains(Flag.CP_MODE) && !current.renderedHistory[current.cursor].didElevate())) {
                    // Avoid a `sudo sudo` scenario.
                    if (!cmdToRun.startsWith("sudo ")) {
                        cmdToRun += "sudo "
                    }
                } else if (current.flags.contains(Flag.CP_MODE) && current.renderedHistory[current.cursor].didElevate()) {
                    cmdToRun += "sudo -v && "
                } else if (current.flags.contains(Flag.CP_MODE) && current.renderedHistory[current.cursor].wasNeverElevated()) {
                    cmdToRun += "sudo -k && "
                }

                cmdToRun += record.cmd //todo: Could NPE

                val targetFile = if (event is TUIAction.HistoryPasteSelected) tabOutputTempFile else enterOutputTempFile
                writeFileLines(targetFile, cmdToRun)
                current.copy(isRunning = false)
            }

            is TUIAction.Terminate -> current.copy(
                isRunning = false
            )

            is TUIAction.HistoryResetCursor -> current.copy(
                cursor = current.cursorMax,
                offset = 0
            )

            is TUIAction.FilterPreviousMode -> current.copy(filterMode = current.filterMode.previous())
            is TUIAction.FilterNextMode -> current.copy(filterMode = current.filterMode.next())
            is TUIAction.MoveToDirectory -> current.toggleFlag(
                flag = Flag.MTD_FLAG,
                onOnBlock = {
                    it.copy(
                        cwd = current.renderedHistory[current.cursor].startDir,
                        filterMode = FilterModes.FM_DIR_PWD,
                    )
                }, onOffBlock = {
                    it.copy(
                        cwd = pwd,
                        filterMode = FilterModes.FM_DIR_ALL
                    )
                })

            is TUIAction.SudoMode -> current.toggleFlag(flag = Flag.SU_MODE)

            is TUIAction.CopyMode -> current.toggleFlag(flag = Flag.CP_MODE)

            is TUIAction.SessionMode -> current.toggleFlag(flag = Flag.SES_MODE)

            is TUIAction.ChangeActivePlane -> current.copy(activePlane = current.activePlane.nextLooping())

            is TUIAction.Resize -> {
                notcurses_refresh(
                    notcursesCPointer,
                    rowCPointer.ptr.reinterpret(),
                    colCPointer.ptr.reinterpret()
                ) //produce new dim
                size()
            }

            else -> current
        }

        _state.value = next
    }
}
