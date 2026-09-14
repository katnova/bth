package model

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import notcurses.ncplane_set_bg_rgb8
import notcurses.ncplane_set_fg_rgb8

@OptIn(ExperimentalForeignApi::class)
class StrRGB {
    private val mask24 = 16777215uL
    val packed: ULong

    /**
     * Constructs an instance of StrRGB by combining the foreground and background RGB channels
     * into a single packed 64-bit unsigned long value.
     *
     * The packed value is created by encoding the RGB and Alpha values of both foreground and background
     * into specific bit positions using the `pack` method.
     *
     * @param fgR The red channel of the foreground color (0..255).
     * @param fgG The green channel of the foreground color (0..255).
     * @param fgB The blue channel of the foreground color (0..255).
     * @param fgA The alpha channel of the foreground color (0..255).
     * @param bgR The red channel of the background color (0..255).
     * @param bgG The green channel of the background color (0..255).
     * @param bgB The blue channel of the background color (0..255).
     * @param bgA The alpha channel of the background color (0..255).
     */
    constructor(fgR: ULong, fgG: ULong, fgB: ULong, fgA: ULong, bgR: ULong, bgG: ULong, bgB: ULong, bgA: ULong) {
        packed = pack(fgR, fgG, fgB, fgA, bgR, bgG, bgB, bgA)
    }

    /**
     * Constructs a StrRGB instance by combining the foreground and background RGB values
     * into a packed 48-bit representation.
     *
     * The foreground RGB values are extracted from the provided `fgRGB` object, and occupy
     * the upper 24 bits of the packed value. The background RGB values are extracted from
     * the provided `bgRGB` object, and occupy the lower 24 bits.
     *
     * @param fgRGB The RGB representation for the foreground color.
     * @param bgRGB The RGB representation for the background color.
     */
    constructor(fgRGB: RGB, bgRGB: RGB) {
        packed = (((fgRGB.packed.toULong() and mask24) shl 24) or (bgRGB.packed.toULong() and mask24))
    }

    /**
     * Combines the foreground and background RGB and Alpha channels into a single packed 64-bit unsigned long value.
     *
     * The packed value is structured as follows:
     * - Bits 56-63: Foreground Red channel (fgR)
     * - Bits 48-55: Foreground Green channel (fgG)
     * - Bits 40-47: Foreground Blue channel (fgB)
     * - Bits 32-39: Foreground Alpha channel (fgA)
     * - Bits 24-31: Background Red channel (bgR)
     * - Bits 16-23: Background Green channel (bgG)
     * - Bits 8-15: Background Blue channel (bgB)
     * - Bits 0-7: Background Alpha channel (bgA)
     *
     * @param fgR The red channel of the foreground color (0..255).
     * @param fgG The green channel of the foreground color (0..255).
     * @param fgB The blue channel of the foreground color (0..255).
     * @param fgA The alpha channel of the foreground color (0..255).
     * @param bgR The red channel of the background color (0..255).
     * @param bgG The green channel of the background color (0..255).
     * @param bgB The blue channel of the background color (0..255).
     * @param bgA The alpha channel of the background color (0..255).
     * @return A 64-bit packed unsigned long value representing the combined RGB channels.
     */
    fun pack(fgR: ULong, fgG: ULong, fgB: ULong, fgA: ULong, bgR: ULong, bgG: ULong, bgB: ULong, bgA: ULong): ULong {
        return (fgR shl 56) or (fgG shl 48) or (fgB shl 40) or (fgA shl 32) or (bgR shl 24) or (bgG shl 16) or (bgB shl 8) or bgA
    }

    fun fgR() = (packed shr 56) and 255u
    fun fgG() = (packed shr 48) and 255u
    fun fgB() = (packed shr 40) and 255u
    fun fgA() = (packed shr 32) and 255u
    fun bgR() = (packed shr 24) and 255u
    fun bgG() = (packed shr 16) and 255u
    fun bgB() = (packed shr 8) and 255u
    fun bgA() = (packed) and 255u

    /**
     * Updates both the foreground and background colors of the specified ncplane
     * to the RGB values stored in the current instance of the StrRGB class.
     *
     * @param plane Pointer to the ncplane whose foreground and background colors
     * are to be modified.
     */
    fun setAll(plane: CPointer<ncplane>) {
        setForeground(plane)
        setBackground(plane)
    }

    /**
     * Sets the foreground color of the specified ncplane to the RGB values
     * stored in the current instance of the StrRGB class.
     *
     * @param plane Pointer to the ncplane whose foreground color is to be modified.
     */
    fun setForeground(plane: CPointer<ncplane>) {
        ncplane_set_fg_rgb8(plane, this.fgR().toUInt(), this.fgG().toUInt(), this.fgB().toUInt())
    }

    /**
     * Sets the background color of the specified ncplane to the RGB values stored
     * in the current instance of the StrRGB class.
     *
     * @param plane Pointer to the ncplane whose background color is to be modified.
     */
    fun setBackground(plane: CPointer<ncplane>) {
        ncplane_set_bg_rgb8(plane, this.bgR().toUInt(), this.bgG().toUInt(), this.bgB().toUInt())
    }

    /**
     * Extracts the foreground color from the packed RGB value of the current instance.
     *
     * @return The foreground color as an RGB instance, representing the red, green, and blue channels
     *         of the foreground component.
     */
    fun getForeground(): RGB {
        val fgPacked = ((packed shr 24) and mask24).toUInt()
        return RGB(fgPacked)
    }

    /**
     * Extracts the background color from the packed RGB value of the current instance.
     *
     * @return The background color as an RGB instance, representing the red, green, and blue channels
     *         of the background component.
     */
    fun getBackground(): RGB {
        val bgPacked = ((packed) and mask24).toUInt()
        return RGB(bgPacked)
    }

    override fun toString(): String {
        return "fg: \u001B[38;2;${fgR()};${fgG()};${fgB()}m██\u001B[0m r=${
            fgR().toString().padEnd(3)
        }, g=${fgG().toString().padEnd(3)}, b=${fgB().toString().padEnd(3)}, a=${
            fgA().toString().padEnd(3)
        } | bg: \u001B[48;2;${bgR()};${bgG()};${bgB()}m  \u001B[0m r=${bgR().toString().padEnd(3)}, g=${
            bgG().toString().padEnd(3)
        }, b=${bgB().toString().padEnd(3)}, a=${bgA().toString().padEnd(3)}"
    }


}