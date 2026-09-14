package setting

import kotlin.reflect.KClass
import model.KeyBind
import model.StrRGB
import model.XdgDirectories
import setting.Settings.read

enum class SettingOption(val optName: String, val optType: KClass<*>, val default: Any) {
    // VISIBILITY SETTINGS
    TUI_SHOW_EXIT_CODE("tuiShowExitCode", Boolean::class, true),
    TUI_SHOW_CMD_DURATION("tuiShowCommandDuration", Boolean::class, true),
    TUI_SHOW_HOME_DIR_INDICATOR("tuiShowHomeDirIndicator", Boolean::class, true),
    TUI_SHOW_EXEC_DIRECTORY("tuiShowExecDirectory", Boolean::class, false),
    TUI_SHOW_BTH_ID("tuiShowBTHId", Boolean::class, false),
    TUI_SHOW_SUDO_INDICATOR("tuiShowSudoIndicator", Boolean::class, true),

    // INDICATORS
    TUI_HOME_DIR_INDICATOR("tuiHomeDirIndicator", String::class, "~"),
    TUI_NO_EXIT_TIME_INDICATOR("tuiNoExitTimeIndicator", String::class, ""),
    TUI_SUDO_INDICATOR("tuiSudoIndicator", String::class, "#"),
    TUI_CURSOR_INDICATOR("tuiCursorIndicator", String::class, ">"),

    TUI_COLOR_DEFAULT("tuiColor", StrRGB::class, StrRGB(255u, 255u, 255u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_HIGHLIGHTED("tuiColorHighlighted", StrRGB::class, StrRGB(255u, 0u, 255u, 255u, 50u, 50u, 50u, 255u)),
    TUI_COLOR_ZERO_EXIT_CODE("tuiColorZeroExitCode", StrRGB::class, StrRGB(0u, 255u, 0u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_NON_ZERO_EXIT_CODE("tuiColorNonZeroExitCode", StrRGB::class, StrRGB(255u, 0u, 0u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_TIME_SINCE_CMD_INVOCATION("tuiColorTimeSinceCmdInvokation", StrRGB::class, StrRGB(128u, 128u, 128u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_CMD_DURATION("tuiColorCmdDuration", StrRGB::class, StrRGB(128u, 128u, 128u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_HOME_DIR_INDICATOR("tuiColorHomeDirIndicator", StrRGB::class, StrRGB(0u, 255u, 255u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_SUDO_ELEVATED_BEFORE("tuiColorSudoElevatedBefore", StrRGB::class, StrRGB(255u, 255u, 0u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_SUDO_ELEVATED("tuiColorSudoElevated", StrRGB::class, StrRGB(255u, 0u, 0u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_CMD("tuiColorCmd", StrRGB::class, StrRGB(255u, 255u, 255u, 255u, 0u, 0u, 0u, 255u)),
    TUI_COLOR_CMD_HIGHLIGHTED("tuiColorCmdHighlighted", StrRGB::class, StrRGB(255u, 0u, 255u, 255u, 50u, 50u, 50u, 255u)),
    TUI_COLOR_LOGO("tuiColorLogo", StrRGB::class, StrRGB(0u, 0u, 0u, 255u, 255u, 255u, 255u, 255u)),
    TUI_COLOR_MTD_FLAG("tuiColorMtdFlag", StrRGB::class, StrRGB(0u, 0u, 0u, 255u, 0u, 255u, 255u, 255u)),
    TUI_COLOR_SU_MODE("tuiColorSuMode", StrRGB::class, StrRGB(255u, 255u, 255u, 255u, 255u, 0u, 0u, 255u)),
    TUI_COLOR_CP_MODE("tuiColorCpMode", StrRGB::class, StrRGB(0u, 0u, 0u, 255u, 0u, 255u, 0u, 255u)),
    TUI_COLOR_SES_MODE("tuiColorSesMode", StrRGB::class, StrRGB(0u, 0u, 0u, 255u, 191u, 64u, 191u, 255u)),

    // INTERNALS
    TUI_NUM_THREADS("tuiNumThreads", Int::class, 4),
    TUI_INPUT_DEBOUNCE_MS("tuiInputDebounseMs", Int::class, 5),
    LOG_FILE("logFile", String::class, "${XdgDirectories.getStatePath()}/bth.log"),
    SQLITE_DB_PATH("sqliteDbPath", String::class, "${XdgDirectories.getDataPath()}/bth.db"),
    // KEYBINDS
    KEYBIND_HISTORY_SCROLL_UP("HistoryScrollUp", KeyBind::class, "up"),
    KEYBIND_HISTORY_SCROLL_DOWN("HistoryScrollDown", KeyBind::class, "down"),
    KEYBIND_HISTORY_PAGE_UP("HistoryPageUp", KeyBind::class, "pgup"),
    KEYBIND_HISTORY_PAGE_DOWN("HistoryPageDown", KeyBind::class, "pgdown"),
    KEYBIND_HISTORY_EXECUTE_SELECTED("HistoryExecuteSelected", KeyBind::class, "enter"),
    KEYBIND_HISTORY_PASTE_SELECTED("HistoryPasteSelected", KeyBind::class, "tab"),
    KEYBIND_HISTORY_RESET_CURSOR("HistoryResetCursor", KeyBind::class, "home"),
    KEYBIND_INPUT_BUFFER_BACKSPACE("InputBufferBackspace", KeyBind::class, "backspace"),
    KEYBIND_INPUT_BUFFER_CLEAR("InputBufferClear", KeyBind::class, "del"),
    KEYBIND_FILTER_PREVIOUS_MODE("FilterPreviousMode", KeyBind::class, "left"),
    KEYBIND_FILTER_NEXT_MODE("FilterNextMode", KeyBind::class, "right"),
    KEYBIND_TERMINATE("Terminate", KeyBind::class, "esc");

    companion object {
        inline fun <reified T> SettingOption.isDefault(): Boolean = this.default.equals(this.read<T>())
    }
}
