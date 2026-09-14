package model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRWXU
import platform.posix.S_IXGRP
import platform.posix.S_IXOTH
import platform.posix.getenv
import platform.posix.mkdir

@OptIn(ExperimentalForeignApi::class)
object XdgDirectories {
    private fun getEnvOrDefault(envVar: String, defaultPath: String): String {
        val env = getenv(envVar)?.toKString()
        return if (env.isNullOrEmpty()) {
            val home = getenv("HOME")?.toKString() ?: "/tmp"
            "$home/$defaultPath"
        } else {
            env
        }
    }


    val home: String = getenv("HOME")?.toKString()!!

    val configHome: String = getEnvOrDefault("XDG_CONFIG_HOME", ".config")
    val dataHome: String = getEnvOrDefault("XDG_DATA_HOME", ".local/share")
    val cacheHome: String = getEnvOrDefault("XDG_CACHE_HOME", ".cache")
    val stateHome: String = getEnvOrDefault("XDG_STATE_HOME", ".local/state")
    
    fun getConfigPath(): String = "$configHome/bth"
    fun getDataPath(): String = "$dataHome/bth"
    fun getCachePath(): String = "$cacheHome/bth"
    fun getStatePath(): String = "$stateHome/bth"

    fun ensureAll() {
        ensureDirectory(getStatePath())
        ensureFile("${getStatePath()}/bth.log")
        ensureDirectory(getConfigPath())
        ensureDirectory(getDataPath())
    }

    // todo: Probably shouldn't be here
    fun ensureDirectory(path: String) {
        mkdir(path, (S_IRWXU or S_IRGRP or S_IXGRP or S_IROTH or S_IXOTH).toUInt())
        // todo: review: Creates with 755 permissions, ignores error if exists
    }

    // todo: Probably shouldn't be here
    fun ensureFile(path: String) {
        platform.posix.fopen(path, "a")?.let { file ->
            platform.posix.fclose(file)
        }
    }
}
