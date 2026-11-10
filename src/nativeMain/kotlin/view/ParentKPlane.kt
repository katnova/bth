package view

import buildinfo.BuildInfo
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import model.DrawableLineBuilder
import model.FilterModes
import model.Flag
import model.StrRGB
import model.StyledString
import notcurses.ncplane_erase
import notcurses.ncplane_putstr_yx
import notcurses.ncplane_rounded_box_sized
import setting.SettingOption
import setting.Settings.read
import model.state.Counts
import model.state.HistoryState
import model.state.QueryState
import model.state.SliceReader
import util.extensions.assertNegativeOrThrow

@OptIn(ExperimentalForeignApi::class)
internal class ParentKPlane(
    ctx: PlaneContext,
    layout: PlaneLayout,
    history: SliceReader<HistoryState>,
    query: SliceReader<QueryState>,
    counts: SliceReader<Counts>,
) : KPlane<ParentKPlane.Model>(ctx, layout) {

    data class Model(
        val cursor: Int, val cursorMax: Int, val offset: Int,
        val filterMode: FilterModes, val flags: Set<Flag>, val totalHists: Long,
    )

    override val model: Flow<Model> = combine(history.state, query.state, counts.state) { historyState, queryState, countsState ->
        Model(historyState.cursor, historyState.cursorMax, historyState.offset, queryState.filterMode, queryState.flags, countsState.totalHists)
    }.distinctUntilChanged()

    override fun draw(model: Model, geo: Geometry) {
        ncplane_erase(plane)
        drawBorder()
        drawCounts(
            model.totalHists,
            super.planeRows.toInt(),
            model.offset,
            model.cursor,
            model.cursorMax,
            geo.rows, //realRows
            geo.cols //realCols
        )
        val dlb = DrawableLineBuilder(
            yIndex = (planeRows.toInt() - 1),
            startingXIndex = 1,
            maxXIndex = (planeCols.toInt() - 1),
            separation = 1
        )
        drawLogo(dlb)
        drawFilterState(dlb, model.filterMode)
        drawFlags(dlb, model.flags)
        dlb.draw(plane)
    }


    private fun drawFlags(dlb: DrawableLineBuilder, flags: Set<Flag>) {
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
        SettingOption.TUI_COLOR_COUNTS.read<StrRGB>().setAll(plane)
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