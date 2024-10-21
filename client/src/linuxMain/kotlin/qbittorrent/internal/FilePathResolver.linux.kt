package qbittorrent.internal

import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.getpwuid
import platform.posix.getuid

internal actual object FilePathResolver {
    actual fun resolve(filePath: String): String {
        return if (filePath.startsWith("~/")) {
            val homeDir = getpwuid(getuid())?.pointed?.pw_dir?.toKString()
            filePath.replaceFirst("~", homeDir?.trimEnd('/') ?: return filePath)
        } else {
            filePath
        }
    }
}