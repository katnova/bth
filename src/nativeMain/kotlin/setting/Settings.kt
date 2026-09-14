package setting

import cinterop.Confuse
import model.KeyBind
import model.XdgDirectories
import store.TUIAction
import util.logError

/**
 * Global BTH settings
 */
object Settings {
    var settings: Map<SettingOption, Any> = mapOf()
    lateinit var keybinds: Map<KeyBind, TUIAction>

    inline fun <reified T> SettingOption.read(): T {
        if(this.optType !== T::class) throw IllegalArgumentException("Cannot read ${this.name} as type ${T::class.simpleName} when it's ${this.optType.simpleName}. Consider using `.read<${this.optType.simpleName}>()`.")
        return settings[this] as T
    }

    fun load(path: String) {
        val confuse = Confuse()
        XdgDirectories.ensureFile(path)
        try {
            val result: Map<SettingOption, Any> = confuse.parse(path)
            settings = result
        } catch (e: NullPointerException) {
            logError("NPE: ${e.message}, ${e.cause}, ${e.stackTraceToString()}")
            throw e
        }

        keybinds = mapOf(
            SettingOption.KEYBIND_HISTORY_SCROLL_UP.read<KeyBind>() to TUIAction.HistoryScrollUp,
            SettingOption.KEYBIND_HISTORY_SCROLL_DOWN.read<KeyBind>() to TUIAction.HistoryScrollDown,
            SettingOption.KEYBIND_HISTORY_PAGE_UP.read<KeyBind>() to TUIAction.HistoryPageUp,
            SettingOption.KEYBIND_HISTORY_PAGE_DOWN.read<KeyBind>() to TUIAction.HistoryPageDown,
            SettingOption.KEYBIND_HISTORY_EXECUTE_SELECTED.read<KeyBind>() to TUIAction.HistoryExecuteSelected,
            SettingOption.KEYBIND_HISTORY_PASTE_SELECTED.read<KeyBind>() to TUIAction.HistoryPasteSelected,
            SettingOption.KEYBIND_HISTORY_RESET_CURSOR.read<KeyBind>() to TUIAction.HistoryResetCursor,
            SettingOption.KEYBIND_INPUT_BUFFER_BACKSPACE.read<KeyBind>() to TUIAction.InputBufferBackspace,
            SettingOption.KEYBIND_INPUT_BUFFER_CLEAR.read<KeyBind>() to TUIAction.InputBufferClear,
            SettingOption.KEYBIND_FILTER_PREVIOUS_MODE.read<KeyBind>() to TUIAction.FilterPreviousMode,
            SettingOption.KEYBIND_FILTER_NEXT_MODE.read<KeyBind>() to TUIAction.FilterNextMode,
            SettingOption.KEYBIND_TERMINATE.read<KeyBind>() to TUIAction.Terminate,
            //todo: Need to support proper parsing of config file for keybinds with modifiers
            KeyBind('d'.code, alt = true) to TUIAction.MoveToDirectory, // alt + d
            KeyBind('s'.code, alt = true) to TUIAction.SudoMode, // alt + s
            KeyBind('c'.code, alt = true) to TUIAction.CopyMode, // alt + c
            KeyBind('a'.code, alt = true) to TUIAction.SessionMode, // alt + a
            KeyBind('x'.code, alt = true) to TUIAction.ChangeActivePlane
        )
    }
}