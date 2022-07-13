package qbittorrent.internal

import kotlinx.cinterop.*
import platform.posix.*

internal actual object FileReader {
    actual fun contentOrNull(filePath: String): ByteArray? {
        val actualFilePath = if (filePath.startsWith("~/")) {
            val homeDir = getpwuid(getuid())?.pointed?.pw_dir?.toKString()
            filePath.replaceFirst("~", homeDir ?: return null)
        } else {
            filePath
        }
        if (access(actualFilePath, F_OK) != 0) return null
        val fd = fopen(actualFilePath, "r")
        return try {
            memScoped {
                fseek(fd, 0, SEEK_END)
                val size = ftell(fd).convert<Int>()
                fseek(fd, 0, SEEK_SET)

                return ByteArray(size + 1).also { buffer ->
                    fread(buffer.refTo(0), 1UL, size.convert(), fd)
                }
            }
        } catch (e: Throwable) {
            null
        } finally {
            fclose(fd)
        }
    }
}
