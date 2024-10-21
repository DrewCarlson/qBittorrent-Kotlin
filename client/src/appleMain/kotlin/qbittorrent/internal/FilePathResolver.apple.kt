package qbittorrent.internal

import platform.Foundation.NSHomeDirectory

internal actual object FilePathResolver {
    actual fun resolve(filePath: String): String {
        return if (filePath.startsWith("~/")) {
            filePath.replaceFirst("~", NSHomeDirectory().trimEnd('/'))
        } else {
            filePath
        }
    }
}