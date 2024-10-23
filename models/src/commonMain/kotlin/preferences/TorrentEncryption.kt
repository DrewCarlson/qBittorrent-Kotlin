package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class TorrentEncryption(val value: Int) {
    PREFER_ENCRYPTION(0),
    FORCE_ENCRYPTION_ON(1),
    FORCE_ENCRYPTION_OFF(2),
}
