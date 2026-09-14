package model

import setting.SettingOption
import setting.Settings.read

enum class Flag(val display: StyledString) {
    MTD_FLAG( // Move to Directory flag / CD Mode / CHDIR mode
        StyledString(
            primaryStyle = SettingOption.TUI_COLOR_MTD_FLAG.read(),
            str = " CD "
        )
    ),
    SU_MODE(
        StyledString(
            primaryStyle = SettingOption.TUI_COLOR_SU_MODE.read(),
            str = " SU "
        )
    ),
    CP_MODE(
        StyledString(
            primaryStyle = SettingOption.TUI_COLOR_CP_MODE.read(),
            str = " CP "
        )
    ),
    SES_MODE(
        StyledString(
            primaryStyle = SettingOption.TUI_COLOR_SES_MODE.read(),
            str = " SES "
        )
    )
}