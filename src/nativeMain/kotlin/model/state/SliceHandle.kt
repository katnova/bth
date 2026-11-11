package model.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SliceHandle<S>(
    private val root: MutableStateFlow<TUIState>,
    scope: CoroutineScope,
    private val get: (TUIState) -> S,
    private val set: (TUIState, S) -> TUIState
) : SliceReader<S> {
    override val state: StateFlow<S> = root.map(get).stateIn(scope, SharingStarted.Eagerly, get(root.value))

    override val value: S
        get() = get(root.value)

    fun update(f: (S) -> S) = root.update { rootState ->
        val old = get(rootState)
        val new = f(old)
        if (new === old) rootState else set(rootState, new)
    }
}