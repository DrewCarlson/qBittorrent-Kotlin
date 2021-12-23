package drewcarlson.qbittorrent

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*

internal object ErrorTransformer : HttpClientPlugin<ErrorTransformer, ErrorTransformer> {

    override val key: AttributeKey<ErrorTransformer> = AttributeKey("ErrorTransformer")

    override fun prepare(block: ErrorTransformer.() -> Unit): ErrorTransformer = this

    override fun install(plugin: ErrorTransformer, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.Render) {
            try {
                proceed()
            } catch (e: Throwable) {
                if (e is ResponseException) {
                    throw QBittorrentException(
                        status = e.response.status.value,
                        body = e.response.bodyAsText(),
                        cause = e,
                    )
                } else {
                    throw QBittorrentException(cause = e)
                }
            }
        }
    }
}
