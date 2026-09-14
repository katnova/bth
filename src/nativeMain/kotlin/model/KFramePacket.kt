package model

abstract class KFramePacket(
    open val realRows: UInt,
    open val realCols: UInt,
    open val activePlane: ActivePlane
)

data class HistoryKFrame(
    val renderedHistory: List<HistoryRow>,
    val cursor: Int,
    override val realRows: UInt,
    override val realCols: UInt,
    override val activePlane: ActivePlane,
) : KFramePacket(realRows, realCols, activePlane) {}