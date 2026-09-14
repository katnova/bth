package util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.posix.fclose
import platform.posix.fopen

/**
 * Writes [content] to a file, replacing any existing contents.
 *
 * @param filePath Path to the file.
 * @param content Data to write.
 * @throws IllegalStateException if the file cannot be opened.
 */
@OptIn(ExperimentalForeignApi::class)
fun writeFileLines(filePath: String, content: String): Unit = memScoped {
    val file = fopen(filePath, "w") ?: error("Cannot open file: $filePath")
    try {
        platform.posix.fputs(content, file)
    } finally {
        fclose(file)
    }
}

/**
 * Appends [content] to a file.
 */
@OptIn(ExperimentalForeignApi::class)
fun appendFileLine(filePath: String, content: String): Unit = memScoped {
    val file = fopen(filePath, "a") ?: error("Cannot open file: $filePath")
    try {
        platform.posix.fputs(content, file)
    } finally {
        fclose(file)
    }
}

