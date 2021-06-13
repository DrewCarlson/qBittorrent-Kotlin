package drewcarlson.qbittorrent

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*

internal object ErrorTransformer : HttpClientFeature<ErrorTransformer, ErrorTransformer> {

    override val key: AttributeKey<ErrorTransformer> = AttributeKey("ErrorTransformer")

    override fun prepare(block: ErrorTransformer.() -> Unit): ErrorTransformer = this

    override fun install(feature: ErrorTransformer, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.Render) {
            try {
                proceed()
            } catch (e: Throwable) {
                if (e is ClientRequestException) {
                    throw QBittorrentException(
                        status = e.response.status.value,
                        body = e.response.readText(),
                        cause = e,
                    )
                } else {
                    throw QBittorrentException(cause = e)
                }
            }
        }
    }
}
