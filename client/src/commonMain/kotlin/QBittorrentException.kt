package qbittorrent

import io.ktor.client.plugins.*

class QBittorrentException : Exception {

    var status: Int? = null
        private set

    var body: String? = null
        private set

    constructor(cause: Throwable?) : super(cause)
    constructor(status: Int, body: String) : super() {
        this.status = status
        this.body = body
    }

    override val message: String
        get() = body ?: (cause as? ClientRequestException)?.message ?: super.message ?: "<no message>"
}
