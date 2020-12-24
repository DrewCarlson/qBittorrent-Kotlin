package drewcarlson.qbittorrrent

import drewcarlson.qbittorrrent.models.MainData
import drewcarlson.qbittorrrent.models.PieceState
import drewcarlson.qbittorrrent.models.GlobalTransferInfo
import drewcarlson.qbittorrrent.models.TorrentProperties
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlin.coroutines.coroutineContext

private const val PARAM_URLS = "urls"
private const val PARAM_SAVE_PATH = "savepath"
private const val PARAM_CATEGORY = "category"
private const val PARAM_ROOT_FOLDER = "root_folder"
private const val PARAM_FIRST_LAST_PIECE = "firstLastPiecePrio"
private const val PARAM_SEQUENTIAL_DOWNLOAD = "sequentialDownload"

private const val MAIN_DATA_SYNC_MS = 1000L

private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * qBittorrent Web API wrapper.
 *
 * https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)
 */
class QBittorrentClient(
    private val baseUrl: String,
    private val username: String = "admin",
    private val password: String = "adminadmin",
    private val mainDataSyncMs: Long = MAIN_DATA_SYNC_MS,
    httpClient: HttpClient = HttpClient(),
) {

    private val syncScope = CoroutineScope(SupervisorJob() + Default)

    private val http = httpClient.config {
        install(FlakyRetry)
        install(QBittorrentAuth) {
            executeAuth(::authenticate)
        }
        Json {
            serializer = KotlinxSerializer(json)
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    private var syncRid = 0 // NOT: Only access in mainDataFlow
    private val mainDataFlow = flow<MainData> {
        while (true) {
            emit(
                http.get("$baseUrl/api/v2/sync/maindata") {
                    parameter("rid", syncRid++)
                }
            )
            delay(mainDataSyncMs)
        }
    }.shareIn(syncScope, SharingStarted.WhileSubscribed())

    private suspend fun authenticate() {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/auth/login",
            formParameters = Parameters.build {
                append("username", username)
                append("password", password)
            }
        ) {
            header("Referer", baseUrl)
        }
    }

    fun syncMainData(): Flow<MainData> {
        return mainDataFlow
    }

    fun torrentFlow(hash: String): Flow<Torrent> {
        var torrentMap: MutableMap<String, JsonElement>? = null
        return mainDataFlow
            .filter { mainData ->
                mainData.torrents.containsKey(hash) ||
                        mainData.torrentsRemoved.contains(hash)
            }
            .mapNotNull { mainData ->
                if (mainData.torrentsRemoved.contains(hash)) {
                    coroutineContext.cancel()
                    null
                } else {
                    torrentMap?.apply {
                        putAll(mainData.torrents[hash] ?: emptyMap())
                    }
                }
            }
            .map { json.decodeFromJsonElement<Torrent>(JsonObject(it)) }
            .onStart {
                repeat(5) {
                    val torrent = getTorrents(hashes = listOf(hash)).firstOrNull()
                    if (torrent != null) {
                        torrentMap = json.encodeToJsonElement(torrent)
                            .jsonObject
                            .toMutableMap()
                        return@onStart emit(torrent)
                    }
                    delay(5_000L)
                }
            }
            .distinctUntilChanged()
            .shareIn(syncScope, SharingStarted.WhileSubscribed(), 1)
    }

    suspend fun addTorrent(configure: AddTorrentBody.() -> Unit) {
        val body = AddTorrentBody().apply(configure)
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/add",
            formParameters = Parameters.build {
                append(PARAM_URLS, body.urls.joinToString("|"))
                append(PARAM_SAVE_PATH, body.savePath)
                append(PARAM_CATEGORY, body.category)
                append(PARAM_FIRST_LAST_PIECE, body.firstLastPiecePriority.toString())
                append(PARAM_SEQUENTIAL_DOWNLOAD, body.sequentialDownload.toString())
                append(PARAM_ROOT_FOLDER, body.rootFolder.toString())
            }
        )
    }

    suspend fun getTorrents(
        filter: TorrentFilter = TorrentFilter.ALL,
        category: String = "",
        sort: String = "",
        reverse: Boolean = false,
        limit: Int = 0,
        offset: Int = 0,
        hashes: List<String> = emptyList()
    ): List<Torrent> {
        return http.get("$baseUrl/api/v2/torrents/info") {
            parameter("filter", filter.name)
            parameter("reverse", reverse)
            parameter("limit", limit)
            parameter("offset", offset)
            if (hashes.isNotEmpty()) {
                parameter("hashes", hashes.joinToString("|"))
            }
            if (category.isNotBlank()) {
                parameter("category", category)
            }
            if (sort.isNotBlank()) {
                parameter("sort", sort)
            }
        }
    }

    suspend fun getTorrentProperties(hash: String): TorrentProperties? {
        return try {
            http.get<TorrentProperties>("$baseUrl/api/v2/torrents/properties") {
                parameter("hash", hash)
            }
        } catch (e: ClientRequestException) {
            if (e.response.status == NotFound) {
                return null
            } else throw e
        }
    }

    suspend fun getGlobalTransferInfo(): GlobalTransferInfo {
        return http.get("$baseUrl/api/v2/transfer/info")
    }

    suspend fun getTorrentFiles(hash: String): List<TorrentFile> {
        val filesWithIds = http.get<JsonArray>("$baseUrl/api/v2/torrents/files") {
            parameter("hash", hash)
        }.mapIndexed { i, fileElement ->
            val id = mapOf("id" to JsonPrimitive(i))
            JsonObject(id + fileElement.jsonObject)
        }

        return json.decodeFromJsonElement(JsonArray(filesWithIds))
    }

    suspend fun getPieceStates(hash: String): List<PieceState> {
        return http.get("$baseUrl/api/v2/torrents/pieceStates") {
            parameter("hash", hash)
        }
    }

    suspend fun getPieceHashes(hash: String): List<String> {
        return http.get("$baseUrl/api/v2/torrents/pieceHashes") {
            parameter("hash", hash)
        }
    }

    suspend fun pauseTorrents(hashes: List<String> = emptyList()) {
        http.get<Unit>("$baseUrl/api/v2/torrents/pause") {
            val value = if (hashes.isEmpty()) "all" else hashes.joinToString("|")
            parameter("hashes", value)
        }
    }

    suspend fun resumeTorrents(hashes: List<String> = emptyList()) {
        http.get<Unit>("$baseUrl/api/v2/torrents/resume") {
            val value = if (hashes.isEmpty()) "all" else hashes.joinToString("|")
            parameter("hashes", value)
        }
    }

    suspend fun deleteTorrents(
        hashes: List<String>,
        deleteFiles: Boolean = false
    ) {
        http.get<Unit>("$baseUrl/api/v2/torrents/delete") {
            val value = if (hashes.isEmpty()) "all" else hashes.joinToString("|")
            parameter("hashes", value)
            parameter("deleteFiles", deleteFiles)
        }
    }

    suspend fun recheckTorrents(hashes: List<String>) {
        http.get<Unit>("$baseUrl/api/v2/torrents/recheck") {
            val value = if (hashes.isEmpty()) "all" else hashes.joinToString("|")
            parameter("hashes", value)
        }
    }

    suspend fun reannounceTorrents(hashes: List<String>) {
        http.get<Unit>("$baseUrl/api/v2/torrents/reannounce") {
            val value = if (hashes.isEmpty()) "all" else hashes.joinToString("|")
            parameter("hashes", value)
        }
    }

    data class AddTorrentBody(
        val urls: MutableList<String> = mutableListOf(),
        var savePath: String = "",
        var category: String = "",
        val tags: MutableList<String> = mutableListOf(),
        var paused: Boolean = false,
        var rootFolder: Boolean = true,
        var sequentialDownload: Boolean = false,
        var firstLastPiecePriority: Boolean = false,
        var rename: String = "",
        var autoTMM: Boolean = false,
        var upLimit: Long = -1,
        var dlLimit: Long = -1,
        var skipChecking: Boolean = false,
        var cookie: String = "",
        // TODO: torrents - Raw data of torrent file. torrents can be presented multiple times.
    )
}
