package qbittorrent

import io.ktor.client.statement.*

class QBittorrentException(
    val response: HttpResponse,
    val body: String,
) : Exception() {
    override val message: String
        get() = body.ifBlank { "${response.status.value}: <no message>" }
}
