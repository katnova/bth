package cli.command

import cli.OptSpec
import cli.ParsedArgs
import model.KeyBind
import model.StrRGB
import model.XdgDirectories
import setting.SettingOption
import setting.SettingOption.Companion.isDefault
import setting.Settings.read
import util.logError
import util.toKeyString

data object Config : Command {
    override val names = setOf("config")
    override val cmdHelp = "Outputs the current BTH configuration"
    override val spec = emptyList<OptSpec<*>>()

    override fun execute(args: ParsedArgs) {
        println("""
            |bth settings
            | 
            | (d) indicates a setting is currently set to it's default value.
            |
            |config file: ${XdgDirectories.getConfigPath()}/bth.ini
        """.trimMargin())
        for (setting in SettingOption.entries) {
            try {
                when (setting.optType) {
                    String::class -> println("[ STRING] -> ${setting.optName}".padEnd(50) + "${if (setting.isDefault<String>()) "(d)" else "   "}: ${setting.read<String>()}")
                    Boolean::class -> println("[BOOLEAN] -> ${setting.optName}".padEnd(50) + "${if (setting.isDefault<Boolean>()) "(d)" else "   "}: ${setting.read<Boolean>()}")
                    KeyBind::class -> println("[KEYBIND] -> ${setting.optName}".padEnd(50) + "${if (setting.isDefault<KeyBind>()) "(d)" else "   "}: ${setting.read<KeyBind>().keybind.toKeyString()}")
                    StrRGB::class -> println("[ STRRGB] -> ${setting.optName}".padEnd(50) + "${if (setting.isDefault<StrRGB>()) "(d)" else "   "}: ${setting.read<StrRGB>()}")
                    Int::class -> println("[    INT] -> ${setting.optName}".padEnd(50) + "${if (setting.isDefault<Int>()) "(d)" else "   "}: ${setting.read<Int>()}")
                }
            } catch (e: IllegalArgumentException) {
                logError("[UNKNOWN] -> ${setting.optName}: ${e.message}", print = true, file = false)
            }
        }
        println("""
            |
            |Notes:
            | - By default, the .ini file is empty. 
            | - When setting rgb values in the .ini file, it must be in the following format:
            |   <setting> = "<fgR>,<fgG>,<fgB>,<fgA>,<bgR>,<bgG>,<bgB>,<bgA>"
        """.trimMargin())
    }
}
