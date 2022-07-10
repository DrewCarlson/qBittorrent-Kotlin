package qbittorrent

internal expect object FileReader {

    fun contentOrNull(filePath: String): ByteArray?
}
