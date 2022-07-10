package qbittorrent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import qbittorrent.QBittorrentClient.Config

internal typealias ExecuteAuth = suspend (HttpClient, String, String, String) -> HttpResponse

internal class QBittorrentAuth {

    private lateinit var config: Config
    private var executeAuth: ExecuteAuth = { _, _, _, _ ->
        throw NotImplementedError("executeAuth must be implemented for QBittorrentAuth.")
    }

    fun setConfig(config: Config) {
        this.config = config
    }

    fun setLogin(block: ExecuteAuth) {
        executeAuth = block
    }

    private val authMutex = Mutex()
    private val lastAuthResponse = MutableStateFlow<HttpResponse?>(null)

    val lastAuthResponseState: StateFlow<HttpResponse?> = lastAuthResponse

    // Check if successfully authed or wait for the next auth result
    suspend fun waitForAuthentication(): Boolean {
        if (lastAuthResponse.value?.isValidForAuth() == true) {
            return true
        }
        return lastAuthResponse.drop(1).first()?.isValidForAuth() ?: false
    }

    suspend fun tryAuth(http: HttpClient, config: Config, executeAuth: ExecuteAuth): Boolean {
        return authMutex.withLock {
            if (lastAuthResponse.value?.isValidForAuth() == true) {
                // Authentication completed while waiting for lock, skip
                return@withLock true
            }

            val response = executeAuth(
                http,
                config.baseUrl,
                config.username,
                config.password
            )

            lastAuthResponse.value = response
            response.isValidForAuth()
        }
    }

    companion object : HttpClientPlugin<QBittorrentAuth, QBittorrentAuth> {

        override val key: AttributeKey<QBittorrentAuth> = AttributeKey("QBittorrentAuth")

        override fun prepare(block: QBittorrentAuth.() -> Unit): QBittorrentAuth =
            QBittorrentAuth().apply(block)

        override fun install(plugin: QBittorrentAuth, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                proceed() // Attempt original request
                when ((subject as? HttpClientCall)?.response?.status) {
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                        // Authentication required
                        if (plugin.tryAuth(scope, plugin.config, plugin.executeAuth)) {
                            // Succeeded, retry original request
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

internal suspend fun HttpResponse.isValidForAuth(): Boolean {
    return status.isSuccess() && bodyAsText().equals("ok.", true)
}

