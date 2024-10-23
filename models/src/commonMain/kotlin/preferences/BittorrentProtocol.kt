package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class BittorrentProtocol(val value: Int) {
    TCP_AND_UTP(0),
    TCP(1),
    UTP(2)
}
