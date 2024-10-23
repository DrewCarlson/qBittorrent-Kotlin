package qbittorrent.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Poko
@Serializable
class Webseed(
    /** URL of the web seed. */
    val url: String,
)
