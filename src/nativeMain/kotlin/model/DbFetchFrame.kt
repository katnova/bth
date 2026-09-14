package model

data class DbFetchFrame(
    val buffer: String,
    val offset: Int,
    val filterMode: FilterModes,
    val cwd: String,
    val flags: Array<Flag>,
    val sessionFilterId: Int,
    val uiRows: UInt,
    val uiCols: UInt
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DbFetchFrame

        if (offset != other.offset) return false
        if (sessionFilterId != other.sessionFilterId) return false
        if (buffer != other.buffer) return false
        if (filterMode != other.filterMode) return false
        if (cwd != other.cwd) return false
        if (!flags.contentEquals(other.flags)) return false
        if (uiRows != other.uiRows) return false
        if (uiCols != other.uiCols) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset
        result = 31 * result + sessionFilterId
        result = 31 * result + buffer.hashCode()
        result = 31 * result + filterMode.hashCode()
        result = 31 * result + cwd.hashCode()
        result = 31 * result + flags.contentHashCode()
        result = 31 * result + uiRows.hashCode()
        result = 31 * result + uiCols.hashCode()
        return result
    }
}