package view

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import model.ActivePlane
import notcurses.ncplane_putstr_yx
import store.TUIState
import store.TuiStore

@OptIn(ExperimentalForeignApi::class)
internal class SettingsKPlane(
    stdPlane: CPointer<ncplane>?,
    tuiStore: TuiStore,
    computeUiRows: (rows: UInt) -> UInt,
    computeUiCols: (cols: UInt) -> UInt,
    computeXCord: (rows: Int) -> Int,
    computeYCord: (cols: Int) -> Int
) : KPlane(
    stdPlane = stdPlane,
    tuiStore = tuiStore,
    computeUiRows = computeUiRows,
    computeUiCols = computeUiCols,
    computeXCord = computeXCord,
    computeYCord = computeYCord,
    activeOnPlane = ActivePlane.SETTINGS_PLANE
) {

    override suspend fun Flow<TUIState>.draw() {
        this.collect {
            submitDraw {
                submitDraw { ncplane_putstr_yx(plane, 0, 1, "Settings plane :D") }
            }
        }
    }
}