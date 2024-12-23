package qbittorrent.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Poko
@Serializable
class TorrentPeers(
    @SerialName("full_update")
    val fullUpdate: Boolean = false,
    val rid: Int = 0,
    @SerialName("show_flags")
    val showFlags: Boolean = false,
    val peers: Map<String, TorrentPeer> = emptyMap(),
    @SerialName("peers_removed")
    val peersRemoved: List<String> = emptyList(),
)
