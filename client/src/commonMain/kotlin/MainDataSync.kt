package qbittorrent

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import qbittorrent.models.MainData

private val emptyArray = buildJsonArray { }

internal class MainDataSync(
    private val http: HttpClient,
    private val config: QBittorrentClient.Config,
    syncScope: CoroutineScope,
) {

    private val syncUrl = "${config.baseUrl}/api/v2/sync/maindata"
    private val atomicSyncRid = AtomicReference(0L)
    private var syncRid: Long
        get() = atomicSyncRid.value
        set(value) {
            atomicSyncRid.value = value
        }

    private val currentState = MutableStateFlow<Pair<MainData?, Throwable?>>(null to null)
    private val isSyncing = currentState.subscriptionCount
        .map { it > 0 }
        .stateIn(syncScope, SharingStarted.Eagerly, false)

    init {
        syncScope.launch {
            while (true) {
                // Wait for the first subscribers
                isSyncing.first { it }
                syncMainData()
            }
        }
    }

    fun isSyncing(): Boolean {
        return isSyncing.value
    }

    fun observeMainData(): Flow<MainData> {
        return currentState
            .onSubscription {
                if (!http.plugin(QBittorrentAuth).tryAuth(http, config, ::login)) {
                    val (_, authError) = currentState.first { it.second != null }
                    throw checkNotNull(authError)
                }
            }
            .transform { (mainData, error) ->
                error?.let { throw it }
                mainData?.let { emit(it) }
            }
    }

    private suspend fun syncMainData() {
        try {
            // Get the current MainData value, fetching the initial data if required
            val (initialMainData, _) = currentState.updateAndGet { (mainData, error) ->
                if (error == null) {
                    (mainData ?: fetchMainData(0)) to null
                } else {
                    // Last request produced an error, try it again
                    fetchMainData<MainData>(syncRid) to null
                }
            }

            delay(config.mainDataSyncMs)

            val mainDataJson = json.encodeToJsonElement(initialMainData).mutateJson()
            // Patch MainData while there is at least one subscriber
            while (isSyncing.value) {
                if (syncRid == Long.MAX_VALUE) syncRid = 0

                // Fetch the next MainData patch and merge into existing model, remove any error
                currentState.value = mainDataJson.applyPatch(fetchMainData(++syncRid)) to null

                delay(config.mainDataSyncMs)
            }
        } catch (e: QBittorrentException) {
            // Failed to fetch patch, keep current MainData and add the error
            currentState.getAndUpdate { (mainData, _) -> mainData to e }
        }
    }

    private suspend inline fun <reified T> fetchMainData(rid: Long): T {
        return http.get(syncUrl) { parameter("rid", rid) }.bodyOrThrow()
    }

    private fun MutableMap<String, JsonElement>.applyPatch(newObject: JsonObject): MainData {
        merge(newObject)
        dropRemoved("torrents")
        dropRemoved("categories")
        dropRemovedStrings("tags")

        // Note: Create new MainData here so that one update event includes
        // identifiers of removed data
        val mainData: MainData = json.decodeFromJsonElement(JsonObject(this))

        put("tags_removed", emptyArray)
        put("torrents_removed", emptyArray)
        put("categories_removed", emptyArray)
        return mainData
    }
}
