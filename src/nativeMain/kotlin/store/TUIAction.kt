package store

sealed interface TUIAction {
    data object HistoryScrollUp : TUIAction
    data object HistoryScrollDown : TUIAction
    data object HistoryPageUp : TUIAction
    data object HistoryPageDown : TUIAction
    data object HistoryExecuteSelected : TUIAction
    data object HistoryPasteSelected : TUIAction
    data object HistoryResetCursor : TUIAction
    data object InputBufferBackspace : TUIAction
    data object InputBufferClear : TUIAction
    data object FilterPreviousMode : TUIAction
    data object FilterNextMode : TUIAction
    data class  InputBufferAppend(val char: Char) : TUIAction

    /**
     * Signal to indicate the TUI should exit.
     */
    data object Terminate : TUIAction
    data object Init : TUIAction

    /**
     * CD to the dir of the currently select cmd
     */
    data object MoveToDirectory : TUIAction

    /**
     * Prepend sudo to the cmd
     */
    data object SudoMode : TUIAction

    /**
     * Copy the CWD and SUDO session type for a cmd
     */

    data object CopyMode : TUIAction

    /**
     * Only show commands from a selected session
     */
    data object SessionMode : TUIAction

    data object Resize : TUIAction

    data object ChangeActivePlane : TUIAction
}