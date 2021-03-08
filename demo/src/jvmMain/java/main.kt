package demo

import drewcarlson.qbittorrent.QBittorrentClient
import io.ktor.client.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.Proxy

fun main(vararg args: String) = runBlocking {
    val client = QBittorrentClient(
        baseUrl = "http://192.168.88.6:8080",
        httpClient = HttpClient {
            engine {
                proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(8888))
            }
        }
    )

    client.syncMainData()
        .collect {

        }

    println("qBittorrent Version: ${client.getVersion()}")
}
