package qbittorrent

import io.ktor.client.statement.*

class QBittorrentException : Exception {

    private var body: String? = null
    var response: HttpResponse? = null
        private set

    constructor(
        response: HttpResponse,
        body: String,
    ) : super() {
        this.response = response
        this.body = body
    }

    constructor(cause: Throwable) : super(cause)

    override val message: String
        get() = if (response == null) {
            super.message ?: "<no message>"
        } else {
            body?.ifBlank { "${response?.status?.value}: <no message>" }.orEmpty()
        }
}
