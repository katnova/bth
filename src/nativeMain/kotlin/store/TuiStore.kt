package store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import model.state.Counts
import model.state.HistoryState
import model.state.QueryState
import model.state.SliceHandle
import model.state.SliceReader
import model.state.TUIState
import model.state.ViewState

class TuiStore(
    scope: CoroutineScope,
    pwd: String,
    inputBuffer: String,
    initialRows: UInt,
    initialCols: UInt,
    initialCursorMax: Int,
    counts: Counts,
) {
    private val _state = MutableStateFlow(
        TUIState(
            view = ViewState(realRows = initialRows, realCols = initialCols),
            query = QueryState(cwd = pwd, inputBuffer = inputBuffer),
            history = HistoryState(cursor = initialCursorMax, cursorMax = initialCursorMax),
            counts = counts,
        )
    )
    val state: StateFlow<TUIState> = _state.asStateFlow()

    val view = SliceHandle(_state, scope, { it.view }) { r, s -> r.copy(view = s) }
    val query: SliceHandle<QueryState> = SliceHandle(_state, scope, { it.query }) { r, s -> r.copy(query = s) }
    val history: SliceHandle<HistoryState> = SliceHandle(_state, scope, { it.history }) { r, s -> r.copy(history = s) }
    val counts: SliceReader<Counts> = SliceHandle(_state, scope, { it.counts }) { r, s -> r.copy(counts = s) }

    fun updateRoot(f: (TUIState) -> TUIState) = _state.update(f)

    val drawChannel: Channel<suspend () -> Unit> = Channel(capacity = Channel.BUFFERED)
}
