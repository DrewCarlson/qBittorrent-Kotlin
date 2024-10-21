package qbittorrent.internal

internal expect object FilePathResolver {
    fun resolve(filePath: String): String
}