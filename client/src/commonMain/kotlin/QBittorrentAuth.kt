package qbittorrent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

internal typealias ExecuteAuth = suspend (HttpClient, String, String, String) -> HttpResponse

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

    companion object : HttpClientPlugin<QBittorrentAuth, QBittorrentAuth> {
        override val key: AttributeKey<QBittorrentAuth> = AttributeKey("QBittorrentAuth")

        override fun prepare(block: QBittorrentAuth.() -> Unit): QBittorrentAuth =
            QBittorrentAuth().apply(block)

        override fun install(plugin: QBittorrentAuth, scope: HttpClient) {
            val authMutex = Mutex()
            val isTokenValid = MutableStateFlow(false)
            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                proceed()
                when ((subject as? HttpClientCall)?.response?.status) {
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                        if (!authMutex.isLocked) {
                            isTokenValid.value = false
                        }
                        authMutex.withLock {
                            if (!isTokenValid.value) {
                                withTimeoutOrNull(20.seconds) {
                                    isTokenValid.value = plugin.executeAuth(
                                        scope,
                                        plugin.config.baseUrl,
                                        plugin.config.username,
                                        plugin.config.password
                                    ).status.isSuccess()
                                }
                            }
                        }
                        if (isTokenValid.value) {
                            val req = HttpRequestBuilder().takeFrom(context)
                            val response = scope.request(req)
                            proceedWith(response.call)
                        }
                    }
                }
            }
        }
    }
}
