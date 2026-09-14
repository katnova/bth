package store

import model.ActivePlane
import model.FilterModes
import model.Flag
import model.HistoryRow

data class TUIState(
    val inputBuffer: String = "",
    val cursor: Int = 0,
    val offset: Int = 0,
    val cursorMax: Int = 0,
    val realRows: UInt = 0u,
    val realCols: UInt = 0u,
    val renderedHistory: List<HistoryRow> = emptyList(),
    val isRunning: Boolean = true,
    val filterMode: FilterModes = FilterModes.FM_DIR_ALL,
    val totalCmds: Long = 0,
    val totalDirs: Long = 0,
    val totalHists: Long = 0,
    val cwd: String = "",
    val activePlane: ActivePlane = ActivePlane.HISTORY_PLANE,
    val flags: Array<Flag> = arrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TUIState

        if (cursor != other.cursor) return false
        if (offset != other.offset) return false
        if (cursorMax != other.cursorMax) return false
        if (isRunning != other.isRunning) return false
        if (totalCmds != other.totalCmds) return false
        if (totalDirs != other.totalDirs) return false
        if (totalHists != other.totalHists) return false
        if (inputBuffer != other.inputBuffer) return false
        if (realRows != other.realRows) return false
        if (realCols != other.realCols) return false
        if (renderedHistory != other.renderedHistory) return false
        if (filterMode != other.filterMode) return false
        if (cwd != other.cwd) return false
        if (activePlane != other.activePlane) return false
        if (!flags.contentEquals(other.flags)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cursor
        result = 31 * result + offset
        result = 31 * result + cursorMax
        result = 31 * result + isRunning.hashCode()
        result = 31 * result + totalCmds.hashCode()
        result = 31 * result + totalDirs.hashCode()
        result = 31 * result + totalHists.hashCode()
        result = 31 * result + inputBuffer.hashCode()
        result = 31 * result + realRows.hashCode()
        result = 31 * result + realCols.hashCode()
        result = 31 * result + renderedHistory.hashCode()
        result = 31 * result + filterMode.hashCode()
        result = 31 * result + cwd.hashCode()
        result = 31 * result + activePlane.hashCode()
        result = 31 * result + flags.contentHashCode()
        return result
    }
}