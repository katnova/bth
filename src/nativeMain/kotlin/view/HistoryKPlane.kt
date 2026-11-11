package view

import kotlin.math.abs
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import model.ActivePlane
import notcurses.ncplane_erase
import model.state.HistoryState
import model.state.SliceHandle
import model.state.InputEvent
import model.state.TUIState

@OptIn(ExperimentalForeignApi::class)
internal class HistoryKPlane(
    ctx: PlaneContext,
    layout: PlaneLayout,
    private val history: SliceHandle<HistoryState>,
) : KPlane<HistoryState>(ctx, layout, ActivePlane.HISTORY_PLANE) {

    override val model: Flow<HistoryState> = history.state

    override fun draw(model: HistoryState, geo: Geometry) {
        if (model.rows.size < geo.rows.toInt()) ncplane_erase(plane)
        model.rows.forEachIndexed { i, row ->
            row.rasterize(yIndex = i, xIndex = 1, cursorPos = model.cursor, maxXIndex = geo.cols.toInt()).draw(plane)
        }
    }

    override fun onAction(action: InputEvent): Boolean {
        when (action) {
            InputEvent.UpArrow -> history.update { it.moveCursor(-1) }
            InputEvent.DownArrow -> history.update { it.moveCursor(1) }
            InputEvent.PageUp -> history.update { it.moveCursor(-5) }
            InputEvent.PageDown -> history.update { it.moveCursor(5) }
            InputEvent.Home -> history.update { it.copy(cursor = it.cursorMax, offset = 0) }
            else -> return false
        }
        return true
    }
}

/**
 * Helper function to calculate new valid cursor positions.
 *
 * Provided a [potential] (i.e. +1, -1, +5, -5), determine the next valid position and update [cursor] in [TUIState].
 */
internal fun HistoryState.moveCursor(potential: Int): HistoryState {
    val result = cursor + potential
    var nextOffset = offset
    if (result > cursorMax && nextOffset > 0 && rows.isNotEmpty()) {
        if (rows.size - 1 == cursorMax) nextOffset -= (result - cursorMax).coerceAtLeast(0)
    } else if (result < 0) {
        nextOffset += abs(result)
    }
    return copy(cursor = result.coerceIn(0, cursorMax), offset = nextOffset)
}