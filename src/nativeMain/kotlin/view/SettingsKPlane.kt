package view

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import model.ActivePlane
import notcurses.ncplane_putstr_yx
import model.state.InputEvent

@OptIn(ExperimentalForeignApi::class)
internal class SettingsKPlane(
    ctx: PlaneContext,
    layout: PlaneLayout,
) : KPlane<Int>(ctx, layout, ActivePlane.SETTINGS_PLANE) {

    private val selected = MutableStateFlow(0)
    override val model: Flow<Int> = selected

    override fun draw(model: Int, geo: Geometry) {
        ncplane_putstr_yx(plane, 0, 1, "Settings plane :D  [selected=$model]")
    }

    override fun onAction(action: InputEvent): Boolean {
        when (action) {
            InputEvent.UpArrow   -> selected.update { (it - 1).coerceAtLeast(0) }
            InputEvent.DownArrow -> selected.update { it + 1 }
            else -> return false
        }
        return true
    }
}