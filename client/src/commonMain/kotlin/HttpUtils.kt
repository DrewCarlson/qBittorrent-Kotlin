package qbittorrent

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal suspend fun HttpResponse.orThrow() {
    if (!status.isSuccess()) {
        throw call.attributes.takeOrNull(ErrorTransformer.KEY_INTERNAL_ERROR)
            ?: QBittorrentException(this, bodyAsText())
    }
}

internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    return if (status.isSuccess()) {
        when (T::class) {
            String::class -> bodyAsText() as T
            else -> body()
        }
    } else {
        throw call.attributes.takeOrNull(ErrorTransformer.KEY_INTERNAL_ERROR)
            ?: QBittorrentException(this, bodyAsText())
    }
}