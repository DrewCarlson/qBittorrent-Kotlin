package qbittorrent.models.preferences

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@Serializable
sealed class ScanDir {

    /**
     * Download to the monitored folder
     */
    @Poko
    @Serializable // 0
    class MonitoredFolder(val scanDir: String) : ScanDir()

    /**
     * Download to the default save path
     */
    @Poko
    @Serializable // 1
    class DefaultSavePath(val scanDir: String) : ScanDir()

    /**
     * Download to this [path]
     */
    @Poko
    @Serializable
    class CustomSavePath(
        val scanDir: String,
        val path: String
    ) : ScanDir()
}
