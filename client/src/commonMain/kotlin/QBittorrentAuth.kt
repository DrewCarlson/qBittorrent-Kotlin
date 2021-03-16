package drewcarlson.qbittorrent

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class QBittorrentAuth {

    private var executeAuth: suspend () -> Unit = {
        throw NotImplementedError("executeAuth must be implemented for QBittorrentAuth.")
    }

    fun executeAuth(block: suspend () -> Unit) {
        executeAuth = block
    }

    companion object : HttpClientFeature<QBittorrentAuth, QBittorrentAuth> {
        override val key: AttributeKey<QBittorrentAuth> = AttributeKey("QBittorrentAuth")

        override fun prepare(block: QBittorrentAuth.() -> Unit): QBittorrentAuth =
            QBittorrentAuth().apply(block)

        override fun install(feature: QBittorrentAuth, scope: HttpClient) {
            val authMutex = Mutex()
            val isTokenValid = MutableStateFlow(false)
            scope.requestPipeline.intercept(HttpRequestPipeline.Render) {
                try {
                    proceed()
                } catch (e: ClientRequestException) {
                    when (e.response.status) {
                        HttpStatusCode.Unauthorized,
                        HttpStatusCode.Forbidden -> {
                            if (!authMutex.isLocked) {
                                isTokenValid.value = false
                            }
                            authMutex.withLock {
                                if (!isTokenValid.value) {
                                    feature.executeAuth()
                                    isTokenValid.value = true
                                }
                            }
                            val req = HttpRequestBuilder().takeFrom(context)
                            val response = scope.request<HttpResponse>(req)
                            proceedWith(response.call)
                        }
                        else -> throw e
                    }
                }
            }
        }
    }
}
