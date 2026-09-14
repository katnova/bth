package model

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import notcurses.ncplane_putstr_yx
import setting.SettingOption
import setting.Settings.read

/**
 * " " + " 0 " + "  23ms "
 *        ^ ^- Set default fg+bg
 *        |- Set fg+bg
 * Result:
 * "  "   - default
 * "0"    - specific fg+bg color
 * "   "  - default
 * "23ms" - specific fg+bg color
 * " "    - default
 *
 * Optimizing:
 * " " + " 0 " + "  23ms " -> "  " + "0" + "   " + "23ms" + " "
 */

//todo: Add reset().
@OptIn(ExperimentalForeignApi::class)
class DrawableLineBuilder(
    val yIndex: Int,
    val startingXIndex: Int,
    val maxXIndex: Int,
    val separation: Int = 1,
    val whitespace: StyledString? = null,
    val highlighted: Boolean = false
) {
    private val ops = mutableListOf<StyledString>()
    private var lineCursor = startingXIndex
    var xIndexBudget = 0

    private fun extractText(str: String): Triple<String, String, String> {
        val firstNonWhitespace = str.indexOfFirst { !it.isWhitespace() }
        val lastNonWhitespace = str.indexOfLast { !it.isWhitespace() }

        var preNonWhitespaceSlice = ""
        var postNonWhitespaceSlice = ""

        // Leading whitespace
        if (firstNonWhitespace > 0) {
            preNonWhitespaceSlice = str.substring(0, firstNonWhitespace)
            xIndexBudget += preNonWhitespaceSlice.length
        }

        // Non-whitespace content
        val nonWhitespaceSlice: String = str.substring(firstNonWhitespace, lastNonWhitespace + 1)
        xIndexBudget += nonWhitespaceSlice.length

        // Trailing whitespace
        if (lastNonWhitespace < str.lastIndex) {
            postNonWhitespaceSlice = str.substring(lastNonWhitespace + 1)
            xIndexBudget += postNonWhitespaceSlice.length
        }

        return Triple(preNonWhitespaceSlice, nonWhitespaceSlice, postNonWhitespaceSlice)
    }

    private fun addOp(styledString: StyledString, checkBackground: Boolean = false) {
        if (whitespace != null && checkBackground && styledString.primaryStyle.getBackground() == SettingOption.TUI_COLOR_DEFAULT.read<StrRGB>().getBackground()) {
            ops.add(
                styledString.copy(
                    primaryStyle = StrRGB(
                        fgRGB = styledString.primaryStyle.getForeground(),
                        bgRGB = whitespace.primaryStyle.getBackground()
                    )
                )
            )
        } else {
            ops.add(styledString)
        }
    }

    fun add(styledString: StyledString): DrawableLineBuilder {
        if (whitespace != null) {
            if (styledString.str.isNotBlank()) {
                val (preWhitespace, content, postWhitespace) = extractText(styledString.str)
                if (preWhitespace.isNotEmpty()) {
                    addOp(whitespace.copy(str = preWhitespace))
                }
                if (content.isNotEmpty()) {
                    addOp(styledString.copy(str = content), true)
                }
                if (postWhitespace.isNotEmpty()) {
                    addOp(whitespace.copy(str = postWhitespace))
                }
            } else {
                addOp(whitespace.copy(str = styledString.str))
            }
            addOp((whitespace.copy(str = whitespace.str.padEnd(separation))))
        } else {
            addOp(styledString)
        }

        return this
    }

    /**
     * Draw this string
     */
    fun draw(plane: CPointer<ncplane>) {
        for ((opIndex, op) in ops.withIndex()) {
            val startCursor = lineCursor

//            val fgApplied = if (highlighted) (if (op.applyAltForeground) op.altStyle.getForeground() else op.primaryStyle.getForeground()) else op.primaryStyle.getForeground()
//            val bgApplied = if (highlighted) op.altStyle.getBackground() else op.primaryStyle.getBackground()

            op.setColors(plane, highlighted)
            ncplane_putstr_yx(plane, yIndex, lineCursor, op.str)

            lineCursor += op.str.length

            if (whitespace == null) {
                lineCursor += separation
            }

            val logOpIndex = "op[$opIndex/${ops.lastIndex}]".padEnd(10)
            val logX = "x=$startCursor..$lineCursor".padEnd(15)
//            logInfo("$logOpIndex y=$yIndex $logX len=${op.str.length} highlighted=$highlighted primary=${op.primaryStyle} alt=${op.altStyle} appliedFg=$fgApplied appliedBg=$bgApplied str='${op.str}'", print = false, file = true)
        }

//        logInfo("ops done count=${ops.size} y=$yIndex x=$lineCursor", print = false, file = true)

        if (whitespace != null) {
            whitespace.setColors(plane, highlighted)
            val fgApplied =
                if (highlighted) (if (whitespace.applyAltForeground) whitespace.altStyle.getForeground() else whitespace.primaryStyle.getForeground()) else whitespace.primaryStyle.getForeground()
            val bgApplied =
                if (highlighted) whitespace.altStyle.getBackground() else whitespace.primaryStyle.getBackground()

            if (maxXIndex > lineCursor) {
//                val paddingStartCursor = lineCursor
                val paddingLength = maxXIndex - lineCursor
                val paddedStr = whitespace.str.padEnd(paddingLength)

                ncplane_putstr_yx(plane, yIndex, lineCursor, paddedStr)

//                val logPadding = "padding".padEnd(10)
//                val logX = "x=$paddingStartCursor..$maxXIndex".padEnd(15)
//                logInfo("$logPadding y=$yIndex $logX len=$paddingLength primary=${whitespace.primaryStyle} alt=${whitespace.altStyle} appliedFg=$fgApplied appliedBg=$bgApplied str='$paddedStr'", print = false, file = true)
            } else {
//                val logPadding = "padding".padEnd(10)
//                logInfo("$logPadding skipped y=$yIndex x=$lineCursor maxX=$maxXIndex", print = false, file = true)
            }
        } else {
//            val logPadding = "padding".padEnd(10)
//            logInfo("$logPadding skipped y=$yIndex reason=no-whitespace x=$lineCursor", print = false, file = true)
        }
    }
}