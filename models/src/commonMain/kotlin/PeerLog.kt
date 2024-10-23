package qbittorrent.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Poko
@Serializable
class PeerLog(
    val id: Int,
    val ip: String,
    val timestamp: Long,
    val blocked: Boolean,
    val reason: String,
)
