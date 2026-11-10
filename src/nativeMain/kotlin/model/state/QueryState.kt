package model.state

import model.FilterModes
import model.Flag

data class QueryState(
    val inputBuffer: String = "",
    val filterMode: FilterModes = FilterModes.FM_DIR_ALL,
    val cwd: String = "",
    val flags: Set<Flag> = emptySet(),          // Set instead of Array, so no custom equals/hashCode
)