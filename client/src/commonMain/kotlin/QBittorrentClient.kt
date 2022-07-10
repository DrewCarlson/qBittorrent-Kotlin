package qbittorrent

import qbittorrent.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.native.concurrent.*

private const val PARAM_URLS = "urls"
private const val PARAM_SAVE_PATH = "savepath"
private const val PARAM_CATEGORY = "category"
private const val PARAM_ROOT_FOLDER = "root_folder"
private const val PARAM_FIRST_LAST_PIECE = "firstLastPiecePrio"
private const val PARAM_SEQUENTIAL_DOWNLOAD = "sequentialDownload"

private const val MAIN_DATA_SYNC_MS = 5000L

@SharedImmutable
private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    useAlternativeNames = false
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
 * @param dispatcher Coroutine dispatcher for flow API processing, defaults to [Dispatchers.Default].
 */
class QBittorrentClient(
    baseUrl: String,
    username: String = "admin",
    password: String = "adminadmin",
    mainDataSyncMs: Long = MAIN_DATA_SYNC_MS,
    httpClient: HttpClient = HttpClient(),
    dispatcher: CoroutineDispatcher = Default,
) {
    companion object {
        const val RATIO_LIMIT_NONE = -1
        const val RATIO_LIMIT_GLOBAL = -2
        const val SEEDING_LIMIT_NONE = -1
        const val SEEDING_LIMIT_GLOBAL = -2

        private val allList = listOf("all")
    }

    internal data class Config(
        val baseUrl: String,
        val username: String,
        val password: String,
        val mainDataSyncMs: Long,
    )

    @Suppress("HttpUrlsUsage")
    private val config = Config(
        if (baseUrl.startsWith("http", true)) baseUrl else "http://$baseUrl",
        username,
        password,
        mainDataSyncMs,
    )

    internal val http: HttpClient
    private val mainDataFlow: SharedFlow<MainData>
    private val syncError = MutableStateFlow<Throwable?>(null)
    private val syncScope = CoroutineScope(SupervisorJob() + dispatcher)

    init {
        val config = config
        val http = httpClient.config {
            install(QBittorrentAuth) {
                setConfig(config)
                setLogin(::login)
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
        }.also { this.http = it }

        val emptyArray = buildJsonArray { }
        var mainData: MainData? = null
        val syncUrl = "${config.baseUrl}/api/v2/sync/maindata"
        var syncRid = 0L
        mainDataFlow = flow {
            syncError.value = null
            http.plugin(QBittorrentAuth).tryAuth(http, config, ::login)
            mainData = mainData ?: http.get(syncUrl) {
                parameter("rid", syncRid++)
            }.bodyOrThrow()

            emit(checkNotNull(mainData))
            delay(mainDataSyncMs)

            val mainDataJson = json.encodeToJsonElement(mainData).mutateJson()
            while (true) {
                if (syncRid == Long.MAX_VALUE) {
                    syncRid = 0
                }
                val mainDataPatch: JsonObject = http.get(syncUrl) {
                    parameter("rid", syncRid++)
                }.bodyOrThrow()

                mainDataJson.apply {
                    merge(mainDataPatch)
                    dropRemoved("torrents")
                    dropRemoved("categories")
                    dropRemovedStings("tags")
                }

                mainData = json.decodeFromJsonElement(JsonObject(mainDataJson))
                emit(checkNotNull(mainData))
                mainDataJson.apply {
                    put("tags_removed", emptyArray)
                    put("torrents_removed", emptyArray)
                    put("categories_removed", emptyArray)
                }
                delay(mainDataSyncMs)
            }
        }.catch { syncError.value = it }
            .shareIn(syncScope, SharingStarted.WhileSubscribed(), 1)
    }

    private suspend fun HttpResponse.orThrow() {
        if (!status.isSuccess()) {
            throw QBittorrentException(this, bodyAsText())
        }
    }

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        return if (status.isSuccess()) {
            when (T::class) {
                String::class -> bodyAsText() as T
                else -> body()
            }
        } else {
            throw QBittorrentException(this, bodyAsText())
        }
    }

    /**
     * Create a session with the provided [username] and [password].
     *
     * NOTE: Calling [login] is not required as authentication is
     * managed internally.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun login(username: String, password: String) {
        http.plugin(QBittorrentAuth).run {
            if (!tryAuth(http, config.copy(username = username, password = password), ::login)) {
                val response = lastAuthResponseState.filterNotNull().first()
                throw QBittorrentException(response, response.bodyAsText())
            }
        }
    }

    /**
     * End the current session.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun logout() {
        http.get("${config.baseUrl}/api/v2/auth/logout").orThrow()
    }

    /**
     * Emits the next [MainData] every [mainDataSyncMs] while subscribed.
     *
     * NOTE: A shared flow is returned so multiple collectors use the
     * same timer and response data.
     */
    fun syncMainData(): Flow<MainData> {
        return mainDataFlow.onSubscription {
            if (!http.plugin(QBittorrentAuth).waitForAuthentication()) {
                throw syncError.filterNotNull().first()
            }
        }
    }

    /**
     * Emits the latest [Torrent] data for the [hash].
     * If the torrent is removed or not found, the flow will be cancelled.
     */
    fun torrentFlow(hash: String): Flow<Torrent> {
        return mainDataFlow
            .onSubscription {
                if (!http.plugin(QBittorrentAuth).waitForAuthentication()) {
                    throw syncError.filterNotNull().first()
                }
            }
            .filter { mainData ->
                mainData.torrents.containsKey(hash) ||
                        mainData.torrentsRemoved.contains(hash)
            }
            .mapNotNull { mainData ->
                if (mainData.torrentsRemoved.contains(hash)) {
                    currentCoroutineContext().cancel()
                    null
                } else {
                    mainData.torrents[hash]
                }
            }
            .distinctUntilChanged()
            .shareIn(syncScope, SharingStarted.WhileSubscribed(), 1)
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun addTorrent(configure: AddTorrentBody.() -> Unit) {
        val body = AddTorrentBody().apply(configure)
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/add",
            formParameters = Parameters.build {
                append(PARAM_URLS, body.urls.joinToString("|"))
                append(PARAM_SAVE_PATH, body.savePath)
                append(PARAM_CATEGORY, body.category)
                append(PARAM_FIRST_LAST_PIECE, body.firstLastPiecePriority.toString())
                append(PARAM_SEQUENTIAL_DOWNLOAD, body.sequentialDownload.toString())
                append(PARAM_ROOT_FOLDER, body.rootFolder.toString())
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getTorrents(
        filter: TorrentFilter = TorrentFilter.ALL,
        category: String = "",
        sort: String = "",
        reverse: Boolean = false,
        limit: Int = 0,
        offset: Int = 0,
        hashes: List<String> = emptyList()
    ): List<Torrent> {
        return http.get("${config.baseUrl}/api/v2/torrents/info") {
            parameter("filter", filter.name.lowercase())
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
        }.bodyOrThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getTorrentProperties(hash: String): TorrentProperties {
        return http.get("${config.baseUrl}/api/v2/torrents/properties") {
            parameter("hash", hash)
        }.bodyOrThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getGlobalTransferInfo(): GlobalTransferInfo {
        return http.get("${config.baseUrl}/api/v2/transfer/info").bodyOrThrow()
    }

    /**
     * Get the [TorrentFile]s for [hash] or an empty list if not yet not available.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getTorrentFiles(hash: String): List<TorrentFile> {
        val filesWithIds = http.get("${config.baseUrl}/api/v2/torrents/files") {
            parameter("hash", hash)
        }.bodyOrThrow<JsonArray>().mapIndexed { i, fileElement ->
            val id = mapOf("id" to JsonPrimitive(i))
            JsonObject(id + fileElement.jsonObject)
        }

        return json.decodeFromJsonElement(JsonArray(filesWithIds))
    }

    /**
     * Get piece states for the torrent at [hash].
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getPieceStates(hash: String): List<PieceState> {
        return http.get("${config.baseUrl}/api/v2/torrents/pieceStates") {
            parameter("hash", hash)
        }.bodyOrThrow()
    }

    /**
     * Get piece hashes for the torrent at [hash].
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getPieceHashes(hash: String): List<String> {
        return http.get("${config.baseUrl}/api/v2/torrents/pieceHashes") {
            parameter("hash", hash)
        }.bodyOrThrow()
    }

    /**
     * Pause one or more torrents
     *
     * @param hashes A single torrent hash, list of torrents, or 'all'.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun pauseTorrents(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/pause") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    /**
     * Resume one or more torrents
     *
     * @param hashes A single torrent hash, list of torrents, or 'all'.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun resumeTorrents(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/resume") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    /**
     * Delete one or more torrents.
     *
     * @param hashes A single torrent hash, list of torrents, or 'all'.
     * @param deleteFiles If true, delete all the torrents files.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun deleteTorrents(
        hashes: List<String>,
        deleteFiles: Boolean = false
    ) {
        http.get("${config.baseUrl}/api/v2/torrents/delete") {
            parameter("hashes", hashes.joinToString("|"))
            parameter("deleteFiles", deleteFiles)
        }.orThrow()
    }

    /**
     * Recheck a torrent in qBittorrent.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun recheckTorrents(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/recheck") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    /**
     * Reannounce a torrent.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun reannounceTorrents(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/reannounce") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    /**
     * Get the qBittorrent application preferences.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getPreferences(): JsonObject =
        http.get("${config.baseUrl}/api/v2/app/preferences").bodyOrThrow()

    /**
     * Set one or more qBittorrent application preferences.
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setPreferences(prefs: JsonObject) {
        http.post("${config.baseUrl}/api/v2/app/setPreferences") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("json", prefs)
            })
        }.orThrow()
    }

    /** Get the application version. */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getVersion(): String = http.get("${config.baseUrl}/api/v2/app/version").bodyOrThrow()

    /** Get the Web API version. */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getApiVersion(): String = http.get("${config.baseUrl}/api/v2/app/webapiVersion").bodyOrThrow()

    /** Get the build info */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getBuildInfo(): BuildInfo = http.get("${config.baseUrl}/api/v2/app/buildInfo").bodyOrThrow()

    /** Shutdown qBittorrent */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun shutdown() {
        http.get("${config.baseUrl}/api/v2/app/shutdown").orThrow()
    }

    /** Get the default torrent save path, ex. /user/home/downloads */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getDefaultSavePath(): String = http.get("${config.baseUrl}/api/v2/app/defaultSavePath").bodyOrThrow()

    /**
     * @param lastKnownId Exclude messages with "message id" <= last_known_id (default: -1)
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getPeerLogs(lastKnownId: Int = -1): List<PeerLog> =
        http.get("${config.baseUrl}/api/v2/log/peers") {
            parameter("last_known_id", lastKnownId)
        }.bodyOrThrow()

    /**
     * @param normal Include normal messages (default: true)
     * @param info Include info messages (default: true)
     * @param warning Include warning messages (default: true)
     * @param critical Include critical messages (default: true)
     * @param lastKnownId Exclude messages with "message id" <= last_known_id (default: -1)
     */
    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getLogs(
        normal: Boolean = true,
        info: Boolean = true,
        warning: Boolean = true,
        critical: Boolean = true,
        lastKnownId: Int = -1
    ): List<LogEntry> =
        http.get("${config.baseUrl}/api/v2/log/main") {
            parameter("normal", normal)
            parameter("info", info)
            parameter("warning", warning)
            parameter("critical", critical)
            parameter("last_known_id", lastKnownId)
        }.bodyOrThrow()

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun editTrackers(hash: String, originalUrl: String, newUrl: String) {
        http.get("${config.baseUrl}/api/v2/torrents/editTracker") {
            parameter("hash", hash)
            parameter("origUrl", originalUrl)
            parameter("newUrl", newUrl)
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun addTrackers(hash: String, urls: List<String>) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/addTrackers",
            formParameters = Parameters.build {
                append("hash", hash)
                append("urls", urls.joinToString("\n"))
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun removeTrackers(hash: String, urls: List<String>) {
        http.get("${config.baseUrl}/api/v2/torrents/removeTrackers") {
            parameter("hash", hash)
            parameter("urls", urls.joinToString("|"))
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun increasePriority(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/increasePrio") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun decreasePriority(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/decreasePrio") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun maxPriority(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/topPrio") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun minPriority(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/bottomPrio") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setFilePriority(hash: String, ids: List<Int>, priority: Int) {
        http.get("${config.baseUrl}/api/v2/torrents/filePrio") {
            parameter("hash", hash)
            parameter("id", ids.joinToString("|"))
            parameter("priority", priority)
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getTorrentDownloadLimit(hashes: List<String> = allList): Map<String, Long> {
        return http.submitForm(
            "${config.baseUrl}/api/v2/torrents/downloadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
            }
        ).bodyOrThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setTorrentDownloadLimit(hashes: List<String> = allList) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/downloadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setTorrentShareLimits(hashes: List<String> = allList, ratioLimit: Float, seedingTimeLimit: Long) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/setShareLimits",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("ratioLimit", ratioLimit.toString())
                append("seedingTimeLimit", seedingTimeLimit.toString())
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getTorrentUploadLimit(hashes: List<String> = allList): Map<String, Long> {
        return http.submitForm(
            "${config.baseUrl}/api/v2/torrents/uploadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
            }
        ).bodyOrThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setTorrentUploadLimit(hashes: List<String> = allList, limit: Long) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/setUploadLimit",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("limit", limit.toString())
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setTorrentLocation(hashes: List<String> = allList, location: String) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/setLocation",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("location", location)
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setTorrentName(hash: String, name: String) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/rename",
            formParameters = Parameters.build {
                append("hash", hash)
                append("name", name)
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setTorrentCategory(hashes: List<String> = allList, category: String) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/setCategory",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("category", category)
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getCategories(): List<Category> {
        return http.get("${config.baseUrl}/api/v2/torrents/categories")
            .bodyOrThrow<Map<String, Category>>()
            .values
            .toList()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun createCategory(name: String, savePath: String) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/createCategory",
            formParameters = Parameters.build {
                append("category", name)
                append("savePath", savePath)
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun editCategory(name: String, savePath: String) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/editCategory",
            formParameters = Parameters.build {
                append("category", name)
                append("savePath", savePath)
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun removeCategories(names: List<String>) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/removeCategory",
            formParameters = Parameters.build {
                appendAll("category", names)
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun addTorrentTags(hashes: List<String> = allList, tags: List<String>) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/addTags",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("tags", tags.joinToString(","))
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun removeTorrentTags(hashes: List<String> = allList, tags: List<String>) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/removeTags",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("tags", tags.joinToString(","))
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun getTags(): List<String> = http.get("${config.baseUrl}/api/v2/torrents/tags").bodyOrThrow()

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun createTags(tags: List<String>) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/createTags",
            formParameters = Parameters.build {
                append("tags", tags.joinToString(","))
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun deleteTags(tags: List<String>) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/deleteTags",
            formParameters = Parameters.build {
                append("tags", tags.joinToString(","))
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setAutoTorrentManagement(hashes: List<String> = allList, enabled: Boolean) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/setAutoManagement",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("enabled", enabled.toString())
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun toggleSequentialDownload(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/toggleSequentialDownload") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun toggleFirstLastPriority(hashes: List<String> = allList) {
        http.get("${config.baseUrl}/api/v2/torrents/toggleFirstLastPiecePrio") {
            parameter("hashes", hashes.joinToString("|"))
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setForceStart(hashes: List<String> = allList, value: Boolean) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/setForceStart",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("value", value.toString())
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun setSuperSeeding(hashes: List<String> = allList, value: Boolean) {
        http.submitForm(
            "${config.baseUrl}/api/v2/torrents/setSuperSeeding",
            formParameters = Parameters.build {
                append("hashes", hashes.joinToString("|"))
                append("value", value.toString())
            }
        ).orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun renameFile(hash: String, id: Int, newName: String) {
        http.get("${config.baseUrl}/api/v2/torrents/renameFile") {
            parameter("hash", hash)
            parameter("id", id)
            parameter("name", newName)
        }.orThrow()
    }

    @Throws(QBittorrentException::class, CancellationException::class)
    suspend fun addPeers(hashes: List<String>, peers: List<String>) {
        http.get("${config.baseUrl}/api/v2/torrents/addPeers") {
            parameter("hashes", hashes.joinToString("|"))
            parameter("peers", peers.joinToString("|"))
        }.orThrow()
    }
}

private suspend fun login(http: HttpClient, baseUrl: String, username: String, password: String): HttpResponse {
    return http.submitForm(
        "$baseUrl/api/v2/auth/login",
        formParameters = Parameters.build {
            append("username", username)
            append("password", password)
        }
    ) {
        header("Referer", baseUrl)
    }
}
