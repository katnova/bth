package controller

import cnames.structs.notcurses
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import model.FilterModes
import model.Flag
import model.HistoryRow
import model.state.InputEvent
import model.state.QueryState
import notcurses.notcurses_refresh
import store.TuiStore
import util.extensions.adjustedMax
import util.nextLooping
import util.writeFileLines
import view.moveCursor

@OptIn(ExperimentalForeignApi::class)
class GlobalController(
    private val store: TuiStore,
    private val pwd: String,
    private val tabOutputTempFile: String,
    private val enterOutputTempFile: String,
    private val rowCPointer: UIntVar,
    private val colCPointer: UIntVar,
    private val nc: CPointer<notcurses>,
) {
    fun handle(action: InputEvent) {
        when (action) {
            InputEvent.Esc         -> store.view.update { it.copy(isRunning = false) }
            InputEvent.ChangeActivePlane -> store.view.update { it.copy(activePlane = it.activePlane.nextLooping()) }

            InputEvent.Resize -> {
                notcurses_refresh(nc, rowCPointer.ptr.reinterpret(), colCPointer.ptr.reinterpret())
                val rows = rowCPointer.value
                val cols = colCPointer.value
                val max = rowCPointer.adjustedMax() - 1
                // Spans two slices, so one root update keeps view and history consistent.
                store.updateRoot { s ->
                    s.copy(
                        view = s.view.copy(realRows = rows, realCols = cols),
                        history = s.history.copy(cursorMax = max).moveCursor(0),
                    )
                }
            }

            InputEvent.MoveToDirectory -> store.updateRoot { stateSnapshot ->
                val query = stateSnapshot.query
                val newQuery = if (Flag.MTD_FLAG in query.flags) {
                    query.copy(flags = query.flags - Flag.MTD_FLAG, cwd = pwd, filterMode = FilterModes.FM_DIR_ALL)
                } else {
                    val dir = stateSnapshot.history.selected?.startDir ?: return@updateRoot stateSnapshot
                    query.copy(flags = query.flags + Flag.MTD_FLAG, cwd = dir, filterMode = FilterModes.FM_DIR_PWD)
                }
                stateSnapshot.copy(query = newQuery)
            }

            InputEvent.Enter, InputEvent.Tab -> {
                val stateSnapshot = store.state.value                     // a snapshot read is enough
                val record = stateSnapshot.history.selected ?: return     // fixes the "todo: Could NPE"
                val target = if (action == InputEvent.Tab) tabOutputTempFile else enterOutputTempFile
                writeFileLines(target, buildCommand(record, stateSnapshot.query))   // side effect, outside any update {}
                store.view.update { it.copy(isRunning = false) }
            }

            else -> Unit
        }
    }

    private fun buildCommand(record: HistoryRow, queryState: QueryState): String {
        var cmd = ""
        if (Flag.MTD_FLAG in queryState.flags || Flag.CP_MODE in queryState.flags) cmd += "cd ${queryState.cwd} && "
        if (Flag.SU_MODE in queryState.flags || (Flag.CP_MODE in queryState.flags && !record.didElevate())) {
            if (!cmd.startsWith("sudo ")) cmd += "sudo "
        } else if (Flag.CP_MODE in queryState.flags && record.didElevate()) {
            cmd += "sudo -v && "
        } else if (Flag.CP_MODE in queryState.flags && record.wasNeverElevated()) {
            cmd += "sudo -k && "
        }
        return cmd + record.cmd
    }
}