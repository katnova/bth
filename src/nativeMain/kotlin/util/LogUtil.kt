package util

import setting.SettingOption
import setting.Settings.read
import kotlin.time.Clock

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

fun logInfo(msg: String, print: Boolean = false, file: Boolean = true) {
    val line = buildLogLine(msg, level = LogLevel.INFO)
    if (print) println(line)
//    if (file) appendFileLine(SettingOption.LOG_FILE.read<String>(), "${Clock.System.now()}$line\n")
}

fun logWarn(msg: String, print: Boolean = false, file: Boolean = true) {
    val line = buildLogLine(msg, level = LogLevel.WARN)
    if (print) println(line)
}

fun logError(msg: String, print: Boolean = false, file: Boolean = true) {
    val line = buildLogLine(msg, level = LogLevel.ERROR)
    if (print) println(line)
}

fun buildLogLine(msg: String, level: LogLevel) = "${Clock.System.now()} \u001B[31m[BTH] ${level.name.padStart(5)}\u001B[0m: $msg"