package qbittorrent.models.preferences

import qbittorrent.models.serialization.GenerateEnumAsIntSerializer

@GenerateEnumAsIntSerializer
enum class UploadSlotsBehavior(val value: Int) {
    FIXED_SLOTS(0),
    UPLOAD_RATE_BASED(1)
}
