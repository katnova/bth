package util.extensions

import model.Flag

fun Set<Flag>.toggle(flag: Flag): Set<Flag> = if (flag in this) this - flag else this + flag