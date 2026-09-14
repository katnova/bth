package model

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import notcurses.ncplane_set_bg_rgb8
import notcurses.ncplane_set_fg_rgb8

@OptIn(ExperimentalForeignApi::class)
class RGB { //todo: Need to support alpha channel
    val packed: UInt

    constructor(r: UInt,g: UInt,b: UInt) {
        packed = (r shl 16) or (g shl 8) or b
    }

    constructor(packed: UInt) {
        this.packed = packed
    }

    fun r() = (packed shr 16) and 255u
    fun g() = (packed shr 8) and 255u
    fun b() = (packed) and 255u

    fun setFg(plane: CPointer<ncplane>) {
        ncplane_set_fg_rgb8(plane, this.r(), this.g(), this.b())
    }

    fun setBg(plane: CPointer<ncplane>) {
        ncplane_set_bg_rgb8(plane, this.r(), this.g(), this.b())
    }

    override fun toString(): String {
        return "\u001B[38;2;${r()};${g()};${b()}m██\u001B[0m r=${r().toString().padEnd(3)}, g=${g().toString().padEnd(3)}, b=${b().toString().padEnd(3)}"
    }
}