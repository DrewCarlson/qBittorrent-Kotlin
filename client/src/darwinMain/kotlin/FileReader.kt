package qbittorrent.internal

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*

@OptIn(UnsafeNumber::class)
internal actual object FileReader {

    actual fun contentOrNull(filePath: String): ByteArray? = memScoped {
        val actualFilePath = if (filePath.startsWith("~/")) {
            filePath.replaceFirst("~", NSHomeDirectory())
        } else {
            filePath
        }
        val fileManager = NSFileManager.defaultManager
        val isDirectory = alloc<BooleanVar>()
        val fileExists = fileManager.fileExistsAtPath(actualFilePath, isDirectory.ptr)
        if (fileExists && !isDirectory.value) {
            try {
                val data = fileManager.contentsAtPath(actualFilePath) ?: return@memScoped null
                ByteArray(data.length.toInt()).apply {
                    usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
                }
            } catch (_: Throwable) {
                null
            }
        } else null
    }
}