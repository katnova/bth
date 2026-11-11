package model.state

import model.HistoryRow

data class HistoryState(
    val rows: List<HistoryRow> = emptyList(),
    val cursor: Int = 0,
    val cursorMax: Int = 0,
    val offset: Int = 0,
) {
    val selected: HistoryRow? get() = rows.getOrNull(cursor)
}