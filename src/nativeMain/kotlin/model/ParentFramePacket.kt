package model

data class ParentFramePacket(
    val filter: FilterModes,
    val totalHists: Long,
    val offset: Int,
    val flags: Array<Flag>,
    val uiRows: UInt,
    val uiCols: UInt,
    val cursorMax: Int,
    val cursor: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ParentFramePacket

        if (totalHists != other.totalHists) return false
        if (offset != other.offset) return false
        if (cursorMax != other.cursorMax) return false
        if (cursor != other.cursor) return false
        if (filter != other.filter) return false
        if (!flags.contentEquals(other.flags)) return false
        if (uiRows != other.uiRows) return false
        if (uiCols != other.uiCols) return false

        return true
    }

    override fun hashCode(): Int {
        var result = totalHists.hashCode()
        result = 31 * result + offset
        result = 31 * result + cursorMax
        result = 31 * result + cursor
        result = 31 * result + filter.hashCode()
        result = 31 * result + flags.contentHashCode()
        result = 31 * result + uiRows.hashCode()
        result = 31 * result + uiCols.hashCode()
        return result
    }
}