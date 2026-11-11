package model.state

data class TUIState(
    val view: ViewState = ViewState(),
    val query: QueryState = QueryState(),
    val history: HistoryState = HistoryState(),
    val counts: Counts = Counts(),
)