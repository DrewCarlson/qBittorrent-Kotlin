package drewcarlson.qbittorrent.models

import drewcarlson.qbittorrent.models.dtos.MainDataDTO
import kotlinx.serialization.json.JsonObject

data class MainData(
    /** Response ID */
    var rid: Long,
    /** Whether the response contains all the data or partial data */
    var fullUpdate: Boolean = false,
    /** Property: torrent hash, value: same as [QTorrent] */
    var torrents: MutableMap<String, JsonObject> = mutableMapOf(),
    /** List of hashes of torrents removed since last request */
    var torrentsRemoved: List<String> = emptyList(),
    /** Info for categories added since last request */
    var categories: JsonObject? = null,
    /** List of categories removed since last request */
    var categoriesRemoved: List<String> = emptyList(),
    /** List of tags added since last request */
    var tags: MutableList<String> = mutableListOf(),
    /** List of tags removed since last request */
    var tagsRemoved: List<String> = emptyList(),
    /** Global transfer info */
    var serverState: ServerState
) {

    fun copyFromDTO(mainDataDTO: MainDataDTO) {
        if (mainDataDTO.rid != null) {
            rid = mainDataDTO.rid
        }
        if (mainDataDTO.fullUpdate != null) {
            fullUpdate = mainDataDTO.fullUpdate
        }
        if (mainDataDTO.torrents != null) {
            torrents.putAll(mainDataDTO.torrents)
        }
        if (mainDataDTO.torrentsRemoved.isNotEmpty()) {
            torrents.keys.removeAll(mainDataDTO.torrentsRemoved)
        }
        torrentsRemoved = mainDataDTO.torrentsRemoved

        if (mainDataDTO.categories != null) { // TODO add static model
            categories = mainDataDTO.categories
        }
        if (mainDataDTO.categoriesRemoved != null) { // TODO remove categories when static Categories model is added
            categoriesRemoved = mainDataDTO.categoriesRemoved
        }
        if (mainDataDTO.tags != null) {
            tags = mainDataDTO.tags as MutableList<String>
        }

        if (mainDataDTO.tagsRemoved.isNotEmpty()) {
            tags.removeAll(mainDataDTO.tagsRemoved)
        }
        tagsRemoved = mainDataDTO.tagsRemoved

        serverState.copyFromDTO(mainDataDTO.serverState)
    }

    companion object {
        fun createFromDTO(mainDataDTO: MainDataDTO): MainData {
            return mainDataDTO.run {
                MainData(
                    rid = rid ?: 0,
                    fullUpdate = fullUpdate!!,
                    torrents = torrents as MutableMap<String, JsonObject>,
                    torrentsRemoved = torrentsRemoved,
                    categories = categories!!,
                    categoriesRemoved = categoriesRemoved,
                    tags = tags as MutableList<String>,
                    tagsRemoved = tagsRemoved,
                    serverState = ServerState.createFromDTO(serverState)
                )
            }
        }
    }
}
