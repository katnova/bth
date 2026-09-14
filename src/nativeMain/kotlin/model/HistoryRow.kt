package model

import exception.BthException
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant
import setting.SettingOption
import setting.Settings.read
import util.logError
import util.toPrettyString

class HistoryRow(
    val id: String,
    val startEpoch: Instant,
    val startDir: String,
    val cmd: String,
    val elevatedBefore: Boolean,
    val elevatedAfter: Boolean?,
    val endDir: String?,
    val exitCode: Int?,
    val endEpoch: Instant?,
    val session: Int,
) {
    fun rasterize(
        yIndex: Int,
        xIndex: Int,
        cursorPos: Int,
        maxXIndex: Int
    ): DrawableLineBuilder { //todo: Consider moving to util
        val highlighted = (yIndex == cursorPos)
        val whitespace = StyledString(
            primaryStyle = SettingOption.TUI_COLOR_DEFAULT.read<StrRGB>(),
            altStyle = SettingOption.TUI_COLOR_HIGHLIGHTED.read<StrRGB>(),
            str = " ",
        )

        val timeStart = Clock.System.now()

        val dlb = DrawableLineBuilder(
            yIndex = yIndex,
            startingXIndex = (xIndex),
            maxXIndex = maxXIndex,
            whitespace = whitespace,
            highlighted = highlighted
        )
            .add( //cursor row
                if (highlighted) {
                    StyledString(
                        altStyle = SettingOption.TUI_COLOR_HIGHLIGHTED.read<StrRGB>(),
                        str = SettingOption.TUI_CURSOR_INDICATOR.read<String>(),
                        applyAltForeground = true
                    )
                } else {
                    //todo: Conf for top, bottom, or both cursor number guides
                    //todo: Conf for colors (more generally, but important here for shade)
                    val distance = abs(yIndex - cursorPos)
                    if (distance < 10) {
                        val shade = (255u - (distance * 15).toUInt())
                        StyledString(
                            primaryStyle = StrRGB(RGB(shade, shade, shade), SettingOption.TUI_COLOR_DEFAULT.read<StrRGB>().getBackground()),
                            str = distance.toString()
                        )
                    } else {
                        StyledString(
                            str = " "
                        )
                    }
                }
            )

        if (SettingOption.TUI_SHOW_BTH_ID.read<Boolean>()) {
            dlb.add(
                StyledString(
                    str = id
                )
            )
        }

        if (SettingOption.TUI_SHOW_EXIT_CODE.read<Boolean>()) {
            dlb.add(
                if (exitCode != null && exitCode.toString() == "0")
                    StyledString(
                        primaryStyle = SettingOption.TUI_COLOR_ZERO_EXIT_CODE.read<StrRGB>(),
                        str = exitCode.toString().padEnd(3, ' ')
                    )
                else
                    StyledString(
                        primaryStyle = SettingOption.TUI_COLOR_NON_ZERO_EXIT_CODE.read<StrRGB>(),
                        str = "".padEnd(3, ' ')
                    )
            )
        }

        if (SettingOption.TUI_SHOW_CMD_DURATION.read<Boolean>()) {
            val durationString: String

            if (endEpoch != null) {
                val duration = endEpoch - startEpoch
                durationString = duration.toPrettyString()
            } else {
                durationString = SettingOption.TUI_NO_EXIT_TIME_INDICATOR.read<String>()
            }
            dlb.add(
                StyledString(
                    primaryStyle = SettingOption.TUI_COLOR_CMD_DURATION.read<StrRGB>(),
                    str = durationString.padStart(5)
                )
            )
        }

        dlb.add(
            StyledString(
                primaryStyle = SettingOption.TUI_COLOR_TIME_SINCE_CMD_INVOCATION.read<StrRGB>(),
                str = (timeStart - startEpoch).toPrettyString().padStart(4),
            )
        )

        if (SettingOption.TUI_SHOW_HOME_DIR_INDICATOR.read<Boolean>()) {
            val dirString = when (startDir) {
                XdgDirectories.home -> SettingOption.TUI_HOME_DIR_INDICATOR.read<String>()
                else -> " "
            }

            dlb.add(
                StyledString(
                    primaryStyle = SettingOption.TUI_COLOR_HOME_DIR_INDICATOR.read<StrRGB>(),
                    str = dirString,
                )
            )
        }

        if (SettingOption.TUI_SHOW_SUDO_INDICATOR.read<Boolean>()) {
            when (elevatedAfter) {
                true if !elevatedBefore -> {
                    dlb.add(
                        StyledString(
                            primaryStyle = SettingOption.TUI_COLOR_SUDO_ELEVATED.read<StrRGB>(),
                            str = SettingOption.TUI_SUDO_INDICATOR.read<String>(),
                        )
                    )
                }

                true if elevatedBefore -> {
                    dlb.add(
                        StyledString(
                            primaryStyle = SettingOption.TUI_COLOR_SUDO_ELEVATED_BEFORE.read<StrRGB>(),
                            str = SettingOption.TUI_SUDO_INDICATOR.read<String>(),
                        )
                    )
                }

                else -> {
                    dlb.add(
                        StyledString(
                            str = " "
                        )
                    )
                }
            }
        }

        if (SettingOption.TUI_SHOW_EXEC_DIRECTORY.read<Boolean>()) dlb.add(
            StyledString(
                str = startDir
            )
        )

        dlb.add(
            StyledString(
                primaryStyle = SettingOption.TUI_COLOR_CMD.read<StrRGB>(),
                altStyle = SettingOption.TUI_COLOR_CMD_HIGHLIGHTED.read<StrRGB>(),
                str = cmd,
                applyAltForeground = true
            )
        )

        try {
            return dlb
        } catch (e: IllegalArgumentException) {
            logError("${e.message}, ${e.cause}: $yIndex, ${(xIndex + 1)}, $maxXIndex")
            throw BthException("${e.message}, ${e.cause}: $yIndex, ${(xIndex + 1)}, $maxXIndex")
        }
    }

    fun didElevate() = (!this.elevatedBefore && this.elevatedAfter == true)

    fun wasNeverElevated() = !(this.elevatedBefore && this.elevatedAfter == true)
}
