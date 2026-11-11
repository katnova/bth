package controller

import db.StorageLayer.nSqlite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.FilterModes
import model.Flag
import model.HistoryRow
import model.state.HistoryState
import model.state.QueryState
import model.state.SliceHandle
import model.state.SliceReader
import model.state.ViewState

class HistoryController(
    private val query: SliceReader<QueryState>,
    private val view: SliceReader<ViewState>,
    private val history: SliceHandle<HistoryState>,
    private val pageSize: () -> Int,// rowCPointer.adjustedMax()
) {
    private data class Request(val query: QueryState, val offset: Int, val sessionId: Long, val rows: UInt)

    fun launch(scope: CoroutineScope) {
        // A new query means page 0. The owner of `offset` reacts, so Input never writes history.
        scope.launch {
            query.state.drop(1).collect { history.update { it.copy(offset = 0) } }
        }

        scope.launch {
            combine(query.state, history.state, view.state) { queryState, historyState, viewState ->
                Request(
                    query = queryState,
                    offset = historyState.offset,
                    sessionId = (if (Flag.SES_MODE in queryState.flags) historyState.selected?.session ?: -1 else -1).toLong(),
                    rows = viewState.realRows,
                )
            }
                .distinctUntilChanged()
                .collectLatest { req ->
                    val results = withContext(Dispatchers.Default) { fetch(req) }
                    history.update { it.withResults(results, pageSize()) }
                }
        }
    }

    private fun fetch(req: Request): List<HistoryRow> = when (req.query.filterMode) {
        FilterModes.FM_DIR_ALL -> nSqlite.history.filter(req.query.inputBuffer, pageSize(), req.offset, req.sessionId.toInt())
        FilterModes.FM_DIR_PWD -> nSqlite.history.filterPwd(req.query.inputBuffer, pageSize(), req.offset, req.query.cwd, req.sessionId.toInt())
        FilterModes.FM_DIR_SUB -> nSqlite.history.filterPwdSub(req.query.inputBuffer, pageSize(), req.offset, req.query.cwd, req.sessionId.toInt())
    }
}

private fun HistoryState.withResults(results: List<HistoryRow>, pageSize: Int): HistoryState {
    val newMax = (results.size - 1).coerceAtLeast(0)
    return copy(
        rows = results,
        cursorMax = newMax,
        cursor = if (cursorMax != newMax) newMax else cursor,
        offset = if (results.size < pageSize) (offset - (pageSize - results.size)).coerceAtLeast(0) else offset,
    )
}