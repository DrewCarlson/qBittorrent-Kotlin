package qbittorrent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

internal object ErrorTransformer : HttpClientPlugin<ErrorTransformer, ErrorTransformer> {

    override val key: AttributeKey<ErrorTransformer> = AttributeKey("ErrorTransformer")

    override fun prepare(block: ErrorTransformer.() -> Unit): ErrorTransformer = this

    override fun install(plugin: ErrorTransformer, scope: HttpClient) {
        scope.sendPipeline.intercept(HttpSendPipeline.Before) {
            proceed()
            val response = (subject as HttpClientCall).response
            if (!response.status.isSuccess()) {
                throw QBittorrentException(
                    response.status.value,
                    "${response.status.value} ${response.status.description}: ${response.bodyAsText()}"
                )
            }
        }
        scope.sendPipeline.intercept(HttpSendPipeline.Before) {
            try {
                proceed()
            } catch (e: Throwable) {
                if (e !is QBittorrentException) {
                    throw QBittorrentException(cause = e)
                }
            }
        }
    }
}
