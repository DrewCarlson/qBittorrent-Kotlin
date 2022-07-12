package qbittorrent

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*

internal class RawCookiesStorage(
    private val cookiesStorage: CookiesStorage
) : CookiesStorage by cookiesStorage {

    override suspend fun get(requestUrl: Url): List<Cookie> {
        return cookiesStorage.get(requestUrl).map { cookie ->
            cookie.copy(encoding = CookieEncoding.RAW)
        }
    }
}