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

internal typealias ExecuteAuth = suspend (HttpClient, String, String, String) -> Unit

internal class QBittorrentAuth {

    private lateinit var config: QBittorrentClient.Config
    private var executeAuth: ExecuteAuth = { _, _, _, _ ->
        throw NotImplementedError("executeAuth must be implemented for QBittorrentAuth.")
    }

    fun setConfig(config: QBittorrentClient.Config) {
        this.config = config
    }

    fun setLogin(block: ExecuteAuth) {
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
                                    feature.executeAuth(
                                        scope,
                                        feature.config.baseUrl,
                                        feature.config.username,
                                        feature.config.password
                                    )
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
