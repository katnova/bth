package model.state

import kotlinx.coroutines.flow.StateFlow

interface SliceReader<S> {
    val state: StateFlow<S>
    val value: S
}