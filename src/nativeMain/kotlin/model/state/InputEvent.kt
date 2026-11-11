package model.state

sealed interface InputEvent {
    data object UpArrow : InputEvent
    data object DownArrow : InputEvent
    data object PageUp : InputEvent
    data object PageDown : InputEvent
    data object Enter : InputEvent
    data object Tab : InputEvent
    data object Home : InputEvent
    data object Backspace : InputEvent
    data object Del : InputEvent
    data object LeftArrow : InputEvent
    data object RightArrow : InputEvent
    data class  Char(val char: kotlin.Char) : InputEvent

    /**
     * Signal to indicate the TUI should exit.
     */
    data object Esc : InputEvent
    data object Init : InputEvent

    /**
     * CD to the dir of the currently select cmd
     */
    data object MoveToDirectory : InputEvent

    /**
     * Prepend sudo to the cmd
     */
    data object SudoMode : InputEvent

    /**
     * Copy the CWD and SUDO session type for a cmd
     */

    data object CopyMode : InputEvent

    /**
     * Only show commands from a selected session
     */
    data object SessionMode : InputEvent

    data object Resize : InputEvent

    data object ChangeActivePlane : InputEvent
}