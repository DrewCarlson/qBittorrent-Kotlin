package qbittorrent.models.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProxyType {
    @SerialName("None")
    NONE,
    HTTP,
    SOCKS5,
    SOCKS4,
}
