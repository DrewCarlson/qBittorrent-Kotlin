package qbittorrent.internal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import qbittorrent.QBittorrentClient.Config
import qbittorrent.login

internal class AuthHandler {

    lateinit var config: Config
    // mutex which guards any request to the login endpoint
    private val authMutex = Mutex()
    // the last http response object received while authenticating
    private val lastAuthResponse = MutableStateFlow<HttpResponse?>(null)
    val lastAuthResponseState: StateFlow<HttpResponse?> = lastAuthResponse

    suspend fun tryAuth(http: HttpClient, config: Config): Boolean {
        return authMutex.withLock {
            if (lastAuthResponse.value?.isValidForAuth() == true) {
                // Authentication completed while waiting for lock, skip
                return@withLock true
            }

            val (baseUrl, username, password) = config
            val response = login(http, baseUrl, username, password)

            lastAuthResponse.value = response
            yield()
            response.isValidForAuth()
        }
    }

    companion object : HttpClientPlugin<AuthHandler, AuthHandler> {

        override val key: AttributeKey<AuthHandler> = AttributeKey("QBittorrentAuth")

        override fun prepare(block: AuthHandler.() -> Unit): AuthHandler =
            AuthHandler().apply(block)

        override fun install(plugin: AuthHandler, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                val isLogin = context.url.pathSegments.lastOrNull() == "login"
                if (isLogin) {
                    // Attempting login, do not modify the request
                    return@intercept
                }

                // Does the request have the SID cookie
                if (context.cookies().none { it.name == "SID" }) {
                    // No SID, authenticate before user request
                    plugin.tryAuth(scope, plugin.config)
                }

                // Attempt user's request, authentication may or may not have been successful,
                // or it the session may have become invalid.  In any case make one last attempt.
                val call = proceed() as? HttpClientCall ?: return@intercept
                if (call.response.status == Forbidden) {
                    plugin.lastAuthResponse.value = call.response
                    // Authentication required
                    if (plugin.tryAuth(scope, plugin.config)) {
                        // Authentication Succeeded, retry original request
                        proceedWith(scope.request(HttpRequestBuilder().takeFrom(context)).call)
                    }
                }
            }
        }
    }

    private suspend fun HttpResponse.isValidForAuth(): Boolean {
        return status.isSuccess() && bodyAsText().equals("ok.", true)
    }
}