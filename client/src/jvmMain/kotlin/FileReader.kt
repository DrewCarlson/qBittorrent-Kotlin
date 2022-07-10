package qbittorrent

import java.io.File

internal actual object FileReader {
    actual fun contentOrNull(filePath: String): ByteArray? {
        val file = File(filePath)
        return if (file.exists()) {
            try {
                file.readBytes()
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }
}
