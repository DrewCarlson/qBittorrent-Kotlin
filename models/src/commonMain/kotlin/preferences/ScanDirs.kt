package qbittorrent.models.preferences

import kotlinx.serialization.Serializable

@Serializable
sealed class ScanDir {

    /**
     * Download to the monitored folder
     */
    @Serializable // 0
    data class MonitoredFolder(val scanDir: String) : ScanDir()

    /**
     * Download to the default save path
     */
    @Serializable // 1
    data class DefaultSavePath(val scanDir: String) : ScanDir()

    /**
     * Download to this [path]
     */
    @Serializable
    data class CustomSavePath(
        val scanDir: String,
        val path: String
    ) : ScanDir()
}
