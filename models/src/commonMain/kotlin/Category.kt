package qbittorrent.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Poko
@Serializable
class Category(
    val name: String,
    val savePath: String,
)
