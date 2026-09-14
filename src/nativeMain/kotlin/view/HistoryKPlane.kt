package view

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import model.ActivePlane
import model.HistoryRow
import notcurses.ncplane_erase
import store.TUIState
import store.TuiStore

@OptIn(ExperimentalForeignApi::class)
internal class HistoryKPlane(
    stdPlane: CPointer<ncplane>?,
    tuiStore: TuiStore,
    computeUiRows: (rows: UInt) -> UInt,
    computeUiCols: (cols: UInt) -> UInt,
    computeXCord: (rows: Int) -> Int,
    computeYCord: (cols: Int) -> Int,
) : KPlane(
    stdPlane = stdPlane,
    tuiStore = tuiStore,
    computeUiRows = computeUiRows,
    computeUiCols = computeUiCols,
    computeXCord = computeXCord,
    computeYCord = computeYCord,
    keySelector = {
        listOf(
            it.renderedHistory,
            it.cursor,
            it.realRows,
            it.realCols,
            it.activePlane
        )
    },
    activeOnPlane = ActivePlane.HISTORY_PLANE
) {

    override suspend fun Flow<TUIState>.draw() {
        this.collect {
            submitDraw {
                if (it.renderedHistory.size < super.planeRows.toInt()) ncplane_erase(plane)
                drawHistory(it.renderedHistory, it.cursor, super.planeCols.toInt())
            }
        }
    }

    private fun drawHistory(rows: List<HistoryRow>, caretPos: Int, maxXIndex: Int) {
        for ((index, value) in rows.withIndex()) {
            value.rasterize(yIndex = index, xIndex = 1, cursorPos = caretPos, maxXIndex = maxXIndex).draw(plane)
        }
    }
}