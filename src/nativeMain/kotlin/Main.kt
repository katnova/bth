import cli.CommandDispatcher
import exception.BthException
import kotlinx.cinterop.ExperimentalForeignApi
import model.XdgDirectories
import platform.posix.exit
import setting.Settings
import util.blockSigwinch
import util.logError
import kotlin.experimental.ExperimentalNativeApi

/**
 * format:
 * bth <COMMAND>
 */

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
fun main(args: Array<String>) {
    // Block SIGWINCH process-wide as early as possible so none of our own threads can be
    // interrupted by it.
    blockSigwinch()

    try {
        XdgDirectories.ensureAll()
        Settings.load("${XdgDirectories.getConfigPath()}/bth.ini")

        CommandDispatcher.execute(args)
    } catch (e: BthException) {
        logError(e.message.toString(), print = true, file = true)
        exit(e.exitCode)
    } catch (e: Throwable) {
        logError("Main caught error, this might be a bug. Message: ${e.message.toString()}. Cause: ${e.cause}. Stacktrace: \n${e.stackTraceToString()}", print = true, file = true)
        exit(70)
    }
}
