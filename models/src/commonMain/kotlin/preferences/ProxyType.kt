package qbittorrent.models.preferences

import kotlinx.serialization.SerialName

enum class ProxyType {
    @SerialName("None")
    NONE,
    HTTP,
    SOCKS5,
    SOCKS4,
}
