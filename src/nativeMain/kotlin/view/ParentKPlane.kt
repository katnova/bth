package view

import buildinfo.BuildInfo
import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import model.DrawableLineBuilder
import model.FilterModes
import model.Flag
import model.StyledString
import notcurses.ncplane_erase
import notcurses.ncplane_putstr_yx
import notcurses.ncplane_rounded_box_sized
import notcurses.ncplane_set_bg_rgb8
import notcurses.ncplane_set_fg_rgb8
import setting.SettingOption
import setting.Settings.read
import store.TUIState
import store.TuiStore
import util.extensions.assertNegativeOrThrow

@OptIn(ExperimentalForeignApi::class)
internal class ParentKPlane(
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
            it.filterMode,
            it.totalHists,
            it.realRows,
            it.realCols,
            it.activePlane,
            it.offset,
            it.flags,
            it.cursorMax,
            it.cursor
        )
    }
) {

    override suspend fun Flow<TUIState>.draw() {
        this.collect {
            submitDraw {
                ncplane_erase(plane)
                drawBorder()
                drawCounts(
                    it.totalHists,
                    super.planeRows.toInt(),
                    it.offset,
                    it.cursor,
                    it.cursorMax,
                    it.realRows,
                    it.realCols
                )
                val dlb = DrawableLineBuilder(
                    yIndex = (planeRows.toInt() - 1),
                    startingXIndex = 1,
                    maxXIndex = (planeCols.toInt() - 1),
                    separation = 1
                )
                drawLogo(dlb)
                drawFilterState(dlb, it.filterMode)
                drawFlags(dlb, it.flags)
                dlb.draw(plane)
            }
        }
    }

    private fun drawFlags(dlb: DrawableLineBuilder, flags: Array<Flag>) {
        for (flag in flags) {
            dlb.add(flag.display)
        }
    }

    private fun drawFilterState(dlb: DrawableLineBuilder, filter: FilterModes) {
        dlb.add(
            StyledString(
                primaryStyle = filter.color,
                str = " < ${filter.displayName} > "
            )
        )
    }

    private fun drawCounts(
        hist: Long,
        rows: Int,
        offset: Int,
        cursor: Int,
        cursorMax: Int,
        uiRows: UInt,
        uiCols: UInt
    ) {
        ncplane_set_fg_rgb8(plane, 0u, 0u, 0u)
        ncplane_set_bg_rgb8(plane, 255u, 255u, 255u)
        ncplane_putstr_yx(
            plane,
            0,
            1,
            " $rows $hist +$offset c[$cursor/$cursorMax] ${uiRows}x${uiCols} "
        ).assertNegativeOrThrow("Failed to draw counts.")
    }

    private fun drawLogo(dlb: DrawableLineBuilder) {
        dlb.add(
            StyledString(
                primaryStyle = SettingOption.TUI_COLOR_LOGO.read(),
                str = " BTH v${BuildInfo.VERSION} "
            )
        )
    }

    private fun drawBorder() {
        ncplane_rounded_box_sized(plane, 0.toUShort(), 0u, planeRows, planeCols, 0u)
    }
}