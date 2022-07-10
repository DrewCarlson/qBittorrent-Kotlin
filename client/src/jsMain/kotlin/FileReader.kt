package qbittorrent

internal actual object FileReader {
    actual fun contentOrNull(filePath: String): ByteArray? {
        throw NotImplementedError("Torrent file paths are not supported in JS, use file bytes or HTTP/Magnet urls instead.")
    }
}
