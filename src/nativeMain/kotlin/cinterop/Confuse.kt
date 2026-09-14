package cinterop

import confuse.CFGF_NONE
import confuse.CFG_FILE_ERROR
import confuse.cfg_free
import confuse.cfg_init
import confuse.cfg_opt_t
import confuse.cfg_parse
import confuse.cfg_type_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import model.KeyBind
import model.StrRGB
import setting.SettingOption
import util.logError
import util.toNCKEY

@OptIn(ExperimentalForeignApi::class)
class Confuse {

    /**
     * You ever look at something, and wonder if you're clever or insane?
     */
    fun parse(path: String): Map<SettingOption, Any> {
        return memScoped {
            val opts = allocArray<cfg_opt_t>(SettingOption.entries.size + 1)
            val allocations = mutableMapOf<SettingOption, kotlinx.cinterop.CPointer<*>>()

            for ((index, value) in SettingOption.entries.withIndex()) {
                opts[index].name = value.optName.cstr.getPointer(this)
                when (value.optType) {
                    String::class, KeyBind::class, StrRGB::class -> {
                        opts[index].type = cfg_type_t.CFGT_STR
                        val cell = alloc<CPointerVar<ByteVar>>()
                        allocations[value] = cell.ptr
                        opts[index].simple_value.string = cell.ptr.reinterpret()
                    }

                    Boolean::class -> {
                        opts[index].type = cfg_type_t.CFGT_BOOL
                        val cell = alloc<IntVar>()
                        cell.value = -1
                        allocations[value] = cell.ptr
                        opts[index].simple_value.boolean = cell.ptr.reinterpret()
                    }

                    Int::class -> {
                        opts[index].type = cfg_type_t.CFGT_INT
                        val cell = alloc<IntVar>()
                        allocations[value] = cell.ptr
                        opts[index].simple_value.number = cell.ptr.reinterpret()
                    }
                }
                opts[index].flags = CFGF_NONE
            }

            opts[SettingOption.entries.size].name = null
            opts[SettingOption.entries.size].type = cfg_type_t.CFGT_NONE

            // Initialize configuration, abi
            val cfg = cfg_init(opts, 0)

            // Parse the configuration file, abi
            if (cfg_parse(cfg, path) == CFG_FILE_ERROR) {
                println("Could not open config file $path")
            }

            //todo: Unsafe use of !!
            val result: Map<SettingOption, Any> = SettingOption.entries.associateWith { setting ->
                val ptr = allocations[setting]!! //every possible setting should've been allocated
                when (setting.optType) {
                    String::class -> {
                        ptr.reinterpret<CPointerVar<ByteVar>>().pointed.value?.toKString() ?: setting.default as String
                    }
                    Boolean::class -> {
                        val v = ptr.reinterpret<IntVar>().pointed.value
                        if (v == -1) setting.default as Boolean else v != 0
                    }
                    KeyBind::class -> {
                        KeyBind((ptr.reinterpret<CPointerVar<ByteVar>>().pointed.value?.toKString() ?: setting.default as String).toNCKEY())
                    }
                    Int::class -> {
                        val v = ptr.reinterpret<IntVar>().pointed.value
                        if(v <= 0) setting.default as Int
                        else v
                    }
                    StrRGB::class -> {
                        val str = ptr.reinterpret<CPointerVar<ByteVar>>().pointed.value?.toKString()
                        if (str != null) {
                            try {
                                val parts = str.split(',').map { it.trim().toULong() }
                                if (parts.size == 8) {
                                    StrRGB(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7])
                                } else {
                                    logError("Bad StrRGB value for ${setting.optName}: $str")
                                    setting.default as StrRGB
                                }
                            } catch (e: Exception) {
                                logError("Exception parsing StrRGB value for ${setting.optName} = $str: ${e.message}")
                                setting.default as StrRGB
                            }
                        } else {
                            setting.default as StrRGB
                        }
                    }
                    else -> throw IllegalStateException("Unsupported type: ${setting.optType}")
                }
            }.toMap()

            // Clean up
            cfg_free(cfg)

            result
        }
    }
}

