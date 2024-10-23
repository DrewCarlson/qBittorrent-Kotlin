package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class UtpTcpMixedMode(val value: Int) {
    PREFER_TCP(0),
    PEER_PROPORTIONAL(1)
}
