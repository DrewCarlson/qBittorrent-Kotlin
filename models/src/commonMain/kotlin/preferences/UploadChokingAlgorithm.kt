package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class UploadChokingAlgorithm(val value: Int) {
    ROUND_ROBIN(0),
    FASTEST_UPLOAD(1),
    ANTI_LEECH(2)
}
