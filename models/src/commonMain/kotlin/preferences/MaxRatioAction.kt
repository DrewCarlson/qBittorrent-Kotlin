package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class MaxRatioAction(val value: Int) {
    PAUSE_TORRENT(0),
    REMOVE_TORRENT(1),
}
