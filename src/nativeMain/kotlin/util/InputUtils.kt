package util

import kotlinx.cinterop.ExperimentalForeignApi
import notcurses.NCKEY_BACKSPACE
import notcurses.NCKEY_DEL
import notcurses.NCKEY_DOWN
import notcurses.NCKEY_ENTER
import notcurses.NCKEY_ESC
import notcurses.NCKEY_HOME
import notcurses.NCKEY_LEFT
import notcurses.NCKEY_PGDOWN
import notcurses.NCKEY_PGUP
import notcurses.NCKEY_RIGHT
import notcurses.NCKEY_TAB
import notcurses.NCKEY_UP

@OptIn(ExperimentalForeignApi::class)
fun String.toNCKEY(): Int = when (this.lowercase()) {
    "enter" -> NCKEY_ENTER
    "tab" -> NCKEY_TAB
    "up" -> NCKEY_UP
    "down" -> NCKEY_DOWN
    "left" -> NCKEY_LEFT
    "right" -> NCKEY_RIGHT
    "esc" -> NCKEY_ESC
    "pgup" -> NCKEY_PGUP
    "pgdown" -> NCKEY_PGDOWN
    "home" -> NCKEY_HOME
    "del" -> NCKEY_DEL
    "backspace" -> NCKEY_BACKSPACE
    else -> {
        throw IllegalStateException("Unknown str '$this', cannot map to key")
    }
}

@OptIn(ExperimentalForeignApi::class)
fun Int.toKeyString(): String = when (this) {
    NCKEY_ENTER -> "enter"
    NCKEY_TAB -> "tab"
    NCKEY_UP -> "up"
    NCKEY_DOWN -> "down"
    NCKEY_LEFT -> "left"
    NCKEY_RIGHT -> "right"
    NCKEY_ESC -> "esc"
    NCKEY_PGUP -> "pgup"
    NCKEY_PGDOWN -> "pgdown"
    NCKEY_HOME -> "home"
    NCKEY_DEL -> "del"
    NCKEY_BACKSPACE -> "backspace"
    else -> {
        throw IllegalStateException("Unknown key code '$this', cannot map to string")
    }
}