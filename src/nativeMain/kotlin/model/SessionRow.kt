package model

import kotlin.time.Instant

data class SessionRow(
    val id: Int,
    val startEpoch: Instant,
)
