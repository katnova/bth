package view

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import model.ActivePlane
import model.DrawableLineBuilder
import model.StrRGB
import model.StyledString
import notcurses.ncplane_erase
import store.TUIState
import store.TuiStore

@OptIn(ExperimentalForeignApi::class)
internal class InputKPlane(
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
            it.inputBuffer,
            it.cwd,
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
                ncplane_erase(plane)
                DrawableLineBuilder(
                    yIndex = 0,
                    startingXIndex = 1,
                    maxXIndex = super.planeRows.toInt(),
                ).add(
                    StyledString(
                        primaryStyle = StrRGB(0uL, 0uL, 0uL, 255u, 255uL, 255uL, 255uL, 255u),
                        str = "${it.cwd}:"
                    )
                )
                    .add(
                        StyledString(
                            primaryStyle = StrRGB(0uL, 0uL, 0uL, 255u, 180uL, 180uL, 180uL, 255u),
                            str = it.inputBuffer
                        )
                    )
                    .draw(plane)
            }
        }
    }
}