package drewcarlson.qbittorrent.models

data class AddTorrentBody(
    val urls: MutableList<String> = mutableListOf(),
    var savePath: String = "",
    var category: String = "",
    val tags: MutableList<String> = mutableListOf(),
    var paused: Boolean = false,
    var rootFolder: Boolean = true,
    var sequentialDownload: Boolean = false,
    var firstLastPiecePriority: Boolean = false,
    var rename: String = "",
    var autoTMM: Boolean = false,
    var upLimit: Long = -1,
    var dlLimit: Long = -1,
    var skipChecking: Boolean = false,
    var cookie: String = "",
    // TODO: torrents - Raw data of torrent file. torrents can be presented multiple times.
)
