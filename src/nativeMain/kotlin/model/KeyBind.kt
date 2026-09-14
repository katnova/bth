package model

/**
 * In accordance with notcurses_input(3), rather than relying on the bitmask.
 *
 * I guess it could break certain inputs, but I feel it's unlikely someone would want to incorporate the
 * num-clear into their keybinds.
 */

data class KeyBind(
    val keybind: Int,
    val shift: Boolean = false,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false,
    val supr: Boolean = false,
    val hypr: Boolean = false,
)
