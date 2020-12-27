package drewcarlson.qbittorrrent

import drewcarlson.qbittorrrent.models.*
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

private const val MAIN_DATA_SYNC_MS = 5000L

private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * qBittorrent Web API wrapper.
 *
 * https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1)
 *
 * @param baseUrl The base URL of qBittorrent, ex. http://localhost:9000
 * @param username The qBittorrent username, default: admin
 * @param password The qBittorrent password, default: adminadmin
 * @param mainDataSyncMs The sync endpoint polling rate when subscribed to a [Flow]
 * @param httpClient Custom HTTPClient, useful when a default client engine is not used
 */
class QBittorrentClient(
    private val baseUrl: String,
    private val username: String = "admin",
    private val password: String = "adminadmin",
    private val mainDataSyncMs: Long = MAIN_DATA_SYNC_MS,
    httpClient: HttpClient = HttpClient(),
) {
    companion object {
        const val RATIO_LIMIT_NONE = -1
        const val RATIO_LIMIT_GLOBAL = -2
        const val SEEDING_LIMIT_NONE = -1
        const val SEEDING_LIMIT_GLOBAL = -2
    }

    private val allList = listOf("all")

    private val syncScope = CoroutineScope(SupervisorJob() + Default)

    private val http = httpClient.config {
        install(FlakyRetry)
        install(QBittorrentAuth) {
            executeAuth(::login)
        }
        Json {
            serializer = KotlinxSerializer(json)
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    private var syncRid = 0 // NOTE: Only access in mainDataFlow
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

    /**
     * Create a session with the provided [username] and [password].
     *
     * NOTE: Calling [login] is not required as authentication is
     * managed internally.
     */
    suspend fun login() {
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

    /**
     * End the current session.
     */
    suspend fun logout() {
        http.get<Unit>("$baseUrl/api/v2/auth/logout")
    }

    /**
     * Emits the next [MainData] every [mainDataSyncMs] while subscribed.
     *
     * NOTE: A shared flow is returned so multiple collectors use the
     * same timer and response data.
     */
    fun syncMainData(): Flow<MainData> {
        return mainDataFlow
    }

    /**
     * Emits the latest [Torrent] data for the [hash].
     * If the torrent is removed or not found, the flow will be cancelled.
     */
    fun torrentFlow(hash: String): Flow<Torrent> {
        var torrentMap: MutableMap<String, JsonElement>? = null
        return mainDataFlow
            .filter { mainData ->
                mainData.torrents.containsKey(hash) ||
                    mainData.torrentsRemoved.contains(hash)
            }
            .mapNotNull { mainData ->
                if (mainData.torrentsRemoved.contains(hash)) {
                    currentCoroutineContext().cancel()
                    null
                } else {
                    torrentMap?.apply {
                        putAll(mainData.torrents[hash] ?: emptyMap())
                    }
                }
            }
            .map { json.decodeFromJsonElement<Torrent>(JsonObject(it)) }
            .onStart {
                val torrent = getTorrents(hashes = listOf(hash)).firstOrNull()
                if (torrent == null) {
                    currentCoroutineContext().cancel()
                } else {
                    torrentMap = json.encodeToJsonElement(torrent)
                        .jsonObject
                        .toMutableMap()
                    emit(torrent)
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
            parameter("filter", filter.name.toLowerCase())
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

    /**
     * Get the [TorrentFile]s for [hash] or an empty list if not yet not available.
     */
    suspend fun getTorrentFiles(hash: String): List<TorrentFile> {
        val filesWithIds = http.get<JsonArray>("$baseUrl/api/v2/torrents/files") {
            parameter("hash", hash)
        }.mapIndexed { i, fileElement ->
            val id = mapOf("id" to JsonPrimitive(i))
            JsonObject(id + fileElement.jsonObject)
        }

        return json.decodeFromJsonElement(JsonArray(filesWithIds))
    }

    /**
     * Get piece states for the torrent at [hash].
     */
    suspend fun getPieceStates(hash: String): List<PieceState> {
        return http.get("$baseUrl/api/v2/torrents/pieceStates") {
            parameter("hash", hash)
        }
    }

    /**
     * Get piece hashes for the torrent at [hash].
     */
    suspend fun getPieceHashes(hash: String): List<String> {
        return http.get("$baseUrl/api/v2/torrents/pieceHashes") {
            parameter("hash", hash)
        }
    }

    /**
     * Pause one or more torrents
     *
     * @param hashes A single torrent hash, list of torrents, or 'all'.
     */
    suspend fun pauseTorrents(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/pause") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    /**
     * Resume one or more torrents
     *
     * @param hashes A single torrent hash, list of torrents, or 'all'.
     */
    suspend fun resumeTorrents(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/resume") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    /**
     * Delete one or more torrents.
     *
     * @param hashes A single torrent hash, list of torrents, or 'all'.
     * @param deleteFiles If true, delete all the torrents files.
     */
    suspend fun deleteTorrents(
        hashes: List<String>,
        deleteFiles: Boolean = false
    ) {
        http.get<Unit>("$baseUrl/api/v2/torrents/delete") {
            parameter("hashes", hashes.joinToString("|"))
            parameter("deleteFiles", deleteFiles)
        }
    }

    /**
     * Recheck a torrent in qBittorrent.
     */
    suspend fun recheckTorrents(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/recheck") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    /**
     * Reannounce a torrent.
     */
    suspend fun reannounceTorrents(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/reannounce") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    /**
     * Get the qBittorrent application preferences.
     */
    suspend fun getPreferences(): JsonObject =
        http.get("$baseUrl/api/v2/app/preferences")

    /**
     * Set one or more qBittorrent application preferences.
     */
    suspend fun setPreferences(prefs: JsonObject) {
        http.post<Unit>("$baseUrl/api/v2/app/setPreferences") {
            contentType(ContentType.Application.Json)
            body = buildJsonObject {
                put("json", prefs)
            }
        }
    }

    /** Get the application version. */
    suspend fun getVersion(): String = http.get("$baseUrl/api/v2/app/version")

    /** Get the Web API version. */
    suspend fun getApiVersion(): String = http.get("$baseUrl/api/v2/app/webapiVersion")

    /** Get the build info */
    suspend fun getBuildInfo(): BuildInfo = http.get("$baseUrl/api/v2/app/buildInfo")

    /** Shutdown qBittorrent */
    suspend fun shutdown() = http.get<Unit>("$baseUrl/api/v2/app/shutdown")

    /** Get the default torrent save path, ex. /user/home/downloads */
    suspend fun getDefaultSavePath(): String = http.get("$baseUrl/api/v2/app/defaultSavePath")

    /**
     * @param lastKnownId Exclude messages with "message id" <= last_known_id (default: -1)
     */
    suspend fun getPeerLogs(lastKnownId: Int = -1): List<PeerLog> =
        http.get("$baseUrl/api/v2/log/peers") {
            parameter("last_known_id", lastKnownId)
        }

    /**
     * @param normal Include normal messages (default: true)
     * @param info Include info messages (default: true)
     * @param warning Include warning messages (default: true)
     * @param critical Include critical messages (default: true)
     * @param lastKnownId Exclude messages with "message id" <= last_known_id (default: -1)
     */
    suspend fun getLogs(
        normal: Boolean = true,
        info: Boolean = true,
        warning: Boolean = true,
        critical: Boolean = true,
        lastKnownId: Int = -1
    ): List<LogEntry> =
        http.get("$baseUrl/api/v2/log/main") {
            parameter("normal", normal)
            parameter("info", info)
            parameter("warning", warning)
            parameter("critical", critical)
            parameter("last_known_id", lastKnownId)
        }

    suspend fun editTrackers(hash: String, originalUrl: String, newUrl: String) {
        http.get<Unit>("$baseUrl/api/v2/torrents/editTracker") {
            parameter("hash", hash)
            parameter("origUrl", originalUrl)
            parameter("newUrl", newUrl)
        }
    }

    suspend fun addTrackers(hash: String, urls: List<String>) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/addTrackers",
            formParameters = Parameters.build {
                append("hash", hash)
                append("urls", urls.joinToString("\n"))
            }
        )
    }

    suspend fun removeTrackers(hash: String, urls: List<String>) {
        http.get<Unit>("$baseUrl/api/v2/torrents/removeTrackers") {
            parameter("hash", hash)
            parameter("urls", urls.joinToString("|"))
        }
    }

    suspend fun increasePriority(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/increasePrio") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    suspend fun decreasePriority(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/decreasePrio") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    suspend fun maxPriority(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/topPrio") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    suspend fun minPriority(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/bottomPrio") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    suspend fun setFilePriority(hash: String, ids: List<Int>, priority: Int) {
        http.get<Unit>("$baseUrl/api/v2/torrents/filePrio") {
            parameter("hash", hash)
            parameter("id", ids.joinToString("|"))
            parameter("priority", priority)
        }
    }

    suspend fun getTorrentDownloadLimit(hashes: List<String> = allList): Map<String, Long> {
        return http.submitForm(
            "$baseUrl/api/v2/torrents/downloadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
            }
        )
    }

    suspend fun setTorrentDownloadLimit(hashes: List<String> = allList) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/downloadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
            }
        )
    }

    suspend fun setTorrentShareLimits(hashes: List<String> = allList, ratioLimit: Float, seedingTimeLimit: Long) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/setShareLimits",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("ratioLimit", ratioLimit.toString())
                append("seedingTimeLimit", seedingTimeLimit.toString())
            }
        )
    }

    suspend fun getTorrentUploadLimit(hashes: List<String> = allList): Map<String, Long> {
        return http.submitForm(
            "$baseUrl/api/v2/torrents/uploadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
            }
        )
    }

    suspend fun setTorrentUploadLimit(hashes: List<String> = allList, limit: Long) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/setUploadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("limit", limit.toString())
            }
        )
    }

    suspend fun setTorrentLocation(hashes: List<String> = allList, location: String) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/setLocation",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("location", location)
            }
        )
    }

    suspend fun setTorrentName(hash: String, name: String) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/rename",
            formParameters = Parameters.build {
                append("hash", hash)
                append("name", name)
            }
        )
    }

    suspend fun setTorrentCategory(hashes: List<String> = allList, category: String) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/setCategory",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("category", category)
            }
        )
    }

    suspend fun getCategories(): List<Category> {
        return http.get<Map<String, Category>>("$baseUrl/api/v2/torrents/categories")
            .values
            .toList()
    }

    suspend fun createCategory(name: String, savePath: String) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/createCategory",
            formParameters = Parameters.build {
                append("category", name)
                append("savePath", savePath)
            }
        )
    }

    suspend fun editCategory(name: String, savePath: String) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/editCategory",
            formParameters = Parameters.build {
                append("category", name)
                append("savePath", savePath)
            }
        )
    }

    suspend fun removeCategories(names: List<String>) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/removeCategory",
            formParameters = Parameters.build {
                appendAll("category", names)
            }
        )
    }

    suspend fun addTorrentTags(hashes: List<String> = allList, tags: List<String>) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/addTags",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("tags", tags.joinToString(","))
            }
        )
    }

    suspend fun removeTorrentTags(hashes: List<String> = allList, tags: List<String>) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/removeTags",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("tags", tags.joinToString(","))
            }
        )
    }

    suspend fun getTags(): List<String> = http.get("$baseUrl/api/v2/torrents/tags")

    suspend fun createTags(tags: List<String>) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/createTags",
            formParameters = Parameters.build {
                append("tags", tags.joinToString(","))
            }
        )
    }

    suspend fun deleteTags(tags: List<String>) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/deleteTags",
            formParameters = Parameters.build {
                append("tags", tags.joinToString(","))
            }
        )
    }

    suspend fun setAutoTorrentManagement(hashes: List<String> = allList, enabled: Boolean) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/setAutoManagement",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("enabled", enabled.toString())
            }
        )
    }

    suspend fun toggleSequentialDownload(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/toggleSequentialDownload") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    suspend fun toggleFirstLastPriority(hashes: List<String> = allList) {
        http.get<Unit>("$baseUrl/api/v2/torrents/toggleFirstLastPiecePrio") {
            parameter("hashes", hashes.joinToString("|"))
        }
    }

    suspend fun setForceStart(hashes: List<String> = allList, value: Boolean) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/setForceStart",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("value", value.toString())
            }
        )
    }

    suspend fun setSuperSeeding(hashes: List<String> = allList, value: Boolean) {
        http.submitForm<Unit>(
            "$baseUrl/api/v2/torrents/setSuperSeeding",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("value", value.toString())
            }
        )
    }

    suspend fun renameFile(hash: String, id: Int, newName: String) {
        http.get<Unit>("$baseUrl/api/v2/torrents/renameFile") {
            parameter("hash", hash)
            parameter("id", id)
            parameter("name", newName)
        }
    }

    suspend fun addPeers(hashes: List<String>, peers: List<String>) {
        http.get<Unit>("$baseUrl/api/v2/torrents/addPeers") {
            parameter("hashes", hashes.joinToString("|"))
            parameter("peers", peers.joinToString("|"))
        }
    }
}
