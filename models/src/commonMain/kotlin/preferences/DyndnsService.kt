package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class DyndnsService(val value: Int) {
    DyDNS(0),
    NOIP(1)
}
