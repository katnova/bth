package view

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import model.ActivePlane
import model.DrawableLineBuilder
import model.Flag
import model.StyledString
import notcurses.ncplane_erase
import setting.SettingOption
import setting.Settings.read
import model.state.QueryState
import model.state.SliceHandle
import model.state.InputEvent
import util.extensions.toggle
import util.next
import util.previous

@OptIn(ExperimentalForeignApi::class)
internal class InputKPlane(
    ctx: PlaneContext,
    layout: PlaneLayout,
    private val query: SliceHandle<QueryState>
) : KPlane<InputKPlane.Model>(ctx, layout, ActivePlane.HISTORY_PLANE) {

    data class Model(val cwd: String, val buffer: String)

    override val model: Flow<Model> =
        query.state
            .map { Model(it.cwd, it.inputBuffer) }
            .distinctUntilChanged()

    override fun draw(model: Model, geo: Geometry) {
        ncplane_erase(plane)
        DrawableLineBuilder(yIndex = 0, startingXIndex = 1, maxXIndex = geo.cols.toInt())
            .add(StyledString(primaryStyle = SettingOption.TUI_COLOR_FILTER_INPUT_DIR.read(), str = "${model.cwd}:"))
            .add(StyledString(primaryStyle = SettingOption.TUI_COLOR_FILTER_INPUT.read(), str = model.buffer))
            .draw(plane)
    }

    override fun onAction(action: InputEvent): Boolean {
        when (action) {
            is InputEvent.Char -> query.update { it.copy(inputBuffer = it.inputBuffer + action.char) }
            InputEvent.Backspace   -> query.update { it.copy(inputBuffer = it.inputBuffer.dropLast(1)) }
            InputEvent.Del       -> query.update { it.copy(inputBuffer = "") }
            InputEvent.LeftArrow     -> query.update { it.copy(filterMode = it.filterMode.previous()) }
            InputEvent.RightArrow         -> query.update { it.copy(filterMode = it.filterMode.next()) }
            InputEvent.SudoMode               -> query.update { it.copy(flags = it.flags.toggle(Flag.SU_MODE)) }
            InputEvent.CopyMode               -> query.update { it.copy(flags = it.flags.toggle(Flag.CP_MODE)) }
            InputEvent.SessionMode            -> query.update { it.copy(flags = it.flags.toggle(Flag.SES_MODE)) }
            else -> return false
        }
        return true
    }
}