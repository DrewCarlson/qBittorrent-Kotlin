package qbittorrent.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Poko
@Serializable
class BuildInfo(
    val qt: String,
    val libtorrent: String,
    val boost: String,
    val openssl: String,
    val bitness: Int,
)
