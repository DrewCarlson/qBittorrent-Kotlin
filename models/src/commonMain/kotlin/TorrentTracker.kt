package qbittorrent.models

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import qbittorrent.models.serialization.FlakyIntSerializer
import qbittorrent.models.serialization.TrackerStatusSerializer

@Poko
@Serializable
class TorrentTracker(
    /** The tracker URL. */
    val url: String,
    /** Tracker status */
    @Serializable(with = TrackerStatusSerializer::class)
    val status: TrackerStatus,
    /**
     * Tracker priority tier. Lower tier trackers are tried before higher tiers.
     * Tier numbers are valid when >= 0, < 0 is used as placeholder when tier does not
     * exist for special entries (such as DHT).
     */
    @Serializable(FlakyIntSerializer::class)
    val tier: Int,
    /** Number of peers for current torrent, as reported by the tracker */
    @SerialName("num_peers")
    val numPeers: Int,
    /** Number of seeds for current torrent, as reported by the tracker */
    @SerialName("num_seeds")
    val numSeeds: Int,
    /** Number of leeches for current torrent, as reported by the tracker */
    @SerialName("num_leeches")
    val numLeeches: Int,
    /** Number of completed downloads for current torrent, as reported by the tracker */
    @SerialName("num_downloaded")
    val numDownloaded: Int,
    /** Tracker message (there is no way of knowing what this message is - it's up to tracker admins) */
    val msg: String = "",
)
