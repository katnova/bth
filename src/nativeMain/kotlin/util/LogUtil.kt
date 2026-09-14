package util

import setting.SettingOption
import setting.Settings.read
import kotlin.time.Clock

fun logInfo(msg: String, print: Boolean = false, file: Boolean = true) {
    val line = "\u001B[34m[BTH]  INFO\u001B[0m: $msg"
    if (file) appendFileLine(SettingOption.LOG_FILE.read<String>(), "${Clock.System.now()}$line\n")
    if(print) println(line)
}

fun logWarn(msg: String, print: Boolean = false, file: Boolean = true) {
    val line = "\u001B[33m[BTH]  WARN\u001B[0m: $msg"
    if (file) appendFileLine(SettingOption.LOG_FILE.read<String>(), "${Clock.System.now()}$line\n")
    if (print) println(line)
}

fun logError(msg: String, print: Boolean = false, file: Boolean = true) {
    val line = "\u001B[31m[BTH] ERROR\u001B[0m: $msg"
    if (file) appendFileLine(SettingOption.LOG_FILE.read<String>(), "${Clock.System.now()} $line\n")
    if (print) println(line)
}