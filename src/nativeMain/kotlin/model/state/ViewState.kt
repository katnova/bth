package model.state

import model.ActivePlane

data class ViewState(
    val realRows: UInt = 0u,
    val realCols: UInt = 0u,
    val isRunning: Boolean = true,
    val activePlane: ActivePlane = ActivePlane.HISTORY_PLANE,
)