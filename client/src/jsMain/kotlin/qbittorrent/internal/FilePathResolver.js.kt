package qbittorrent.internal


internal actual object FilePathResolver {
    actual fun resolve(filePath: String): String {
        error("FileSystem torrent uploads are not implemented")
    }
}