package drewcarlson.qbittorrent.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerState(
    /** all time download (bytes) */
    @SerialName("alltime_dl")
    val allTimeDownload: Long? = null,

    /** all time upload (bytes) */
    @SerialName("alltime_ul")
    val allTimeUpload: Long? = null,

    @SerialName("average_time_queue")
    val averageTimeInQueue: Int? = null,

    /** Connection status */
    @SerialName("connection_status")
    val connectionStatus: ConnectionStatus? = null,

    @SerialName("dht_nodes")
    val dhtNodes: Int? = null,

    /** Data downloaded this session (bytes) */
    @SerialName("dl_info_data")
    val dlInfoData: Long? = null,

    /** Global download rate (bytes/s) */
    @SerialName("dl_info_speed")
    val dlInfoSpeed: Long? = null,

    /** Download rate limit (bytes/s) */
    @SerialName("dl_rate_limit")
    val dlRateLimit: Long? = null,

    @SerialName("free_space_on_disk")
    val freeSpace: Long? = null,

    @SerialName("global_ratio")
    val globalShareRatio: String? = null,

    @SerialName("queued_io_jobs")
    val queuedIoJobs: Int? = null,

    /** True if torrent queueing is enabled */
    val queueing: Boolean? = null,

    @SerialName("read_cache_hits")
    val readCacheHits: String? = null,

    @SerialName("read_cache_overload")
    val readCacheOverload: String? = null,

    /** Transfer list refresh interval (milliseconds) */
    @SerialName("refresh_interval")
    val refreshInterval: Int? = null,

    @SerialName("total_buffers_size")
    val totalBuffersSize: Int? = null,

    @SerialName("total_peer_connections")
    val totalPeerConnections: Int? = null,

    @SerialName("total_queued_size")
    val totalQueuedSize: Int? = null,

    @SerialName("total_wasted_session")
    val sessionWaste: Long? = null,

    /** Data uploaded this session (bytes) */
    @SerialName("up_info_data")
    val upInfoData: Long? = null,

    /** Global upload rate (bytes/s) */
    @SerialName("up_info_speed")
    val upInfoSpeed: Long? = null,

    /** Upload rate limit (bytes/s) */
    @SerialName("up_rate_limit")
    val upRateLimit: Int? = null,

    /** True if alternative speed limits are enabled */
    @SerialName("use_alt_speed_limits")
    val useAltSpeedLimits: Boolean? = null,

    @SerialName("write_cache_overload")
    val writeCacheOverload: String? = null,
)