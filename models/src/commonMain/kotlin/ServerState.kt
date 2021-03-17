package drewcarlson.qbittorrent.models

import drewcarlson.qbittorrent.models.dtos.ServerStateDTO

data class ServerState(
    /** all time download (bytes) */
    var allTimeDownload: Long,

    /** all time upload (bytes) */
    var allTimeUpload: Long,
    var averageTimeInQueue: Long,

    /** Connection status */
    var connectionStatus: ConnectionStatus,
    var dhtNodes: Int,

    /** Data downloaded this session (bytes) */
    var dlInfoData: Long,

    /** Global download rate (bytes/s) */
    var dlInfoSpeed: Long,

    /** Download rate limit (bytes/s) */
    var dlRateLimit: Long,
    var freeSpace: Long,
    var globalShareRatio: String,
    var queuedIoJobs: Int,

    /** True if torrent queueing is enabled */
    var queueing: Boolean,
    var readCacheHits: String,
    var readCacheOverload: String,

    /** Transfer list refresh interval (milliseconds) */
    var refreshInterval: Int,
    var totalBuffersSize: Int,
    var totalPeerConnections: Int,
    var totalQueuedSize: Int,
    var sessionWaste: Long,

    /** Data uploaded this session (bytes) */
    var upInfoData: Long,

    /** Global upload rate (bytes/s) */
    var upInfoSpeed: Long,

    /** Upload rate limit (bytes/s) */
    var upRateLimit: Int,

    /** True if alternative speed limits are enabled */
    var useAltSpeedLimits: Boolean,
    var writeCacheOverload: String,
) {
    fun copyFromDTO(serverStateDTO: ServerStateDTO?) {
        if (serverStateDTO?.allTimeDownload != null) {
            allTimeDownload = serverStateDTO.allTimeDownload
        }
        if (serverStateDTO?.allTimeUpload != null) {
            allTimeUpload = serverStateDTO.allTimeUpload
        }
        if (serverStateDTO?.averageTimeInQueue != null) {
            averageTimeInQueue = serverStateDTO.averageTimeInQueue
        }
        if (serverStateDTO?.connectionStatus != null) {
            connectionStatus = serverStateDTO.connectionStatus
        }
        if (serverStateDTO?.dhtNodes != null) {
            dhtNodes = serverStateDTO.dhtNodes
        }
        if (serverStateDTO?.dlInfoData != null) {
            dlInfoData = serverStateDTO.dlInfoData
        }
        if (serverStateDTO?.dlInfoSpeed != null) {
            dlInfoSpeed = serverStateDTO.dlInfoSpeed
        }
        if (serverStateDTO?.dlRateLimit != null) {
            dlRateLimit = serverStateDTO.dlRateLimit
        }
        if (serverStateDTO?.freeSpace != null) {
            freeSpace = serverStateDTO.freeSpace
        }
        if (serverStateDTO?.globalShareRatio != null) {
            globalShareRatio = serverStateDTO.globalShareRatio
        }
        if (serverStateDTO?.queuedIoJobs != null) {
            queuedIoJobs = serverStateDTO.queuedIoJobs
        }
        if (serverStateDTO?.queueing != null) {
            queueing = serverStateDTO.queueing
        }
        if (serverStateDTO?.readCacheHits != null) {
            readCacheHits = serverStateDTO.readCacheHits
        }
        if (serverStateDTO?.readCacheOverload != null) {
            readCacheOverload = serverStateDTO.readCacheOverload
        }
        if (serverStateDTO?.refreshInterval != null) {
            refreshInterval = serverStateDTO.refreshInterval
        }
        if (serverStateDTO?.totalBuffersSize != null) {
            totalBuffersSize = serverStateDTO.totalBuffersSize
        }
        if (serverStateDTO?.totalPeerConnections != null) {
            totalPeerConnections = serverStateDTO.totalPeerConnections
        }
        if (serverStateDTO?.totalQueuedSize != null) {
            totalQueuedSize = serverStateDTO.totalQueuedSize
        }
        if (serverStateDTO?.sessionWaste != null) {
            sessionWaste = serverStateDTO.sessionWaste
        }
        if (serverStateDTO?.upInfoData != null) {
            upInfoData = serverStateDTO.upInfoData
        }
        if (serverStateDTO?.upInfoSpeed != null) {
            upInfoSpeed = serverStateDTO.upInfoSpeed
        }
        if (serverStateDTO?.upRateLimit != null) {
            upRateLimit = serverStateDTO.upRateLimit
        }
        if (serverStateDTO?.useAltSpeedLimits != null) {
            useAltSpeedLimits = serverStateDTO.useAltSpeedLimits
        }
        if (serverStateDTO?.writeCacheOverload != null) {
            writeCacheOverload = serverStateDTO.writeCacheOverload
        }
    }

    companion object {
        fun createFromDTO(serverStateDTO: ServerStateDTO?): ServerState {
            return serverStateDTO!!.run {
                ServerState(
                    allTimeDownload = allTimeDownload!!,
                    allTimeUpload = allTimeUpload!!,
                    averageTimeInQueue = averageTimeInQueue!!,
                    connectionStatus = connectionStatus!!,
                    dhtNodes = dhtNodes!!,
                    dlInfoData = dlInfoData!!,
                    dlInfoSpeed = dlInfoSpeed!!,
                    dlRateLimit = dlRateLimit!!,
                    freeSpace = freeSpace!!,
                    globalShareRatio = globalShareRatio!!,
                    queuedIoJobs = queuedIoJobs!!,
                    queueing = queueing!!,
                    readCacheHits = readCacheHits!!,
                    readCacheOverload = readCacheOverload!!,
                    refreshInterval = refreshInterval!!,
                    totalBuffersSize = totalBuffersSize!!,
                    totalPeerConnections = totalPeerConnections!!,
                    totalQueuedSize = totalQueuedSize!!,
                    sessionWaste = sessionWaste!!,
                    upInfoData = upInfoData!!,
                    upInfoSpeed = upInfoSpeed!!,
                    upRateLimit = upRateLimit!!,
                    useAltSpeedLimits = useAltSpeedLimits!!,
                    writeCacheOverload = writeCacheOverload!!
                )
            }
        }
    }
}