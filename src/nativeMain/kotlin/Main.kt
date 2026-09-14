import cli.CommandDispatcher
import exception.BthException
import kotlinx.cinterop.ExperimentalForeignApi
import model.XdgDirectories
import notcurses.exit
import setting.Settings
import util.blockSigwinch
import util.logError

/**
 * format:
 * bth <COMMAND>
 */

@OptIn(ExperimentalForeignApi::class)
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
        logError(e.message.toString(), print = true, file = true)
        exit(70)
    }
}

