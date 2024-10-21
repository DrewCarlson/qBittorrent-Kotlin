package qbittorrent.internal

internal actual object FilePathResolver {
    actual fun resolve(filePath: String): String {
        return when {
            filePath.startsWith("~/") -> {
                filePath.replaceFirst("~", System.getProperty("user.home").trimEnd('/'))
            }
            filePath.startsWith("%USERPROFILE%") -> {
                filePath.replaceFirst("%USERPROFILE%", System.getProperty("user.home").trimEnd('/'))
            }
            else -> filePath
        }
    }
}