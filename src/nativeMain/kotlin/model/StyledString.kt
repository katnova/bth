package model

import cnames.structs.ncplane
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import setting.SettingOption
import setting.Settings.read

@OptIn(ExperimentalForeignApi::class)
data class StyledString(
    val primaryStyle: StrRGB = SettingOption.TUI_COLOR_DEFAULT.read(),
    val altStyle: StrRGB = SettingOption.TUI_COLOR_HIGHLIGHTED.read(),
    val str: String,
    val applyAltForeground: Boolean = false
) {
    fun setColors(plane: CPointer<ncplane>, highlight: Boolean = false) { //todo: rework, this sucks
        if (highlight) {
            if (applyAltForeground) {
                altStyle.setForeground(plane)
            } else {
               primaryStyle.setForeground(plane)
            }

            altStyle.setBackground(plane)
        } else {
            primaryStyle.setAll(plane)
        }
    }

    override fun toString(): String {
        return "StyledString(primaryStyle=$primaryStyle, altStyle=$altStyle, str='$str', applyAltForeground=$applyAltForeground)"
    }

}