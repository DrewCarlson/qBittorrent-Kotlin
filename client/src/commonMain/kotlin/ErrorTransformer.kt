package qbittorrent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.utils.io.*

@OptIn(InternalAPI::class)
internal object ErrorTransformer : HttpClientPlugin<ErrorTransformer, ErrorTransformer> {

    val KEY_INTERNAL_ERROR = AttributeKey<QBittorrentException>("INTERNAL_ERROR")

    override val key: AttributeKey<ErrorTransformer> = AttributeKey("ErrorTransformer")

    override fun prepare(block: ErrorTransformer.() -> Unit): ErrorTransformer = this

    override fun install(plugin: ErrorTransformer, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.State) {
            try {
                proceed()
            } catch (e: Throwable) {
                proceedWith(HttpClientCall(
                    scope,
                    context.build(),
                    HttpResponseData(
                        statusCode = HttpStatusCode(-1, ""),
                        requestTime = GMTDate(),
                        body = ByteReadChannel(byteArrayOf()),
                        callContext = context.executionContext,
                        headers = Headers.Empty,
                        version = HttpProtocolVersion.HTTP_1_0,
                    )
                ).apply {
                    attributes.put(KEY_INTERNAL_ERROR, QBittorrentException(e))
                })
            }
        }
    }
}
