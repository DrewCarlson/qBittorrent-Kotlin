package drewcarlson.qbittorrrent.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MainData(
    /** Response ID */
    val rid: Long,
    /** Whether the response contains all the data or partial data */
    @SerialName("full_update")
    val fullUpdate: Boolean = false,
    /** Property: torrent hash, value: same as [QTorrent] */
    val torrents: Map<String, JsonObject> = emptyMap(),
    /** List of hashes of torrents removed since last request */
    @SerialName("torrents_removed")
    val torrentsRemoved: List<String> = emptyList(),
    /** Info for categories added since last request */
    val categories: JsonObject? = null,
    /** List of categories removed since last request */
    @SerialName("categories_removed")
    val categoriesRemoved: List<String> = emptyList(),
    /** List of tags added since last request */
    val tags: List<String> = emptyList(),
    /** List of tags removed since last request */
    @SerialName("tags_removed")
    val tagsRemoved: List<String> = emptyList(),
    /** Global transfer info */
    @SerialName("server_state")
    val serverState: JsonObject? = null
)
