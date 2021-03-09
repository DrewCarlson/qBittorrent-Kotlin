package demo

import drewcarlson.qbittorrent.QBittorrentClient
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) = runBlocking {
    val client = QBittorrentClient(
        baseUrl = args.firstOrNull() ?: "http://localhost:9090"
    )

    println("qBittorrent Version: ${client.getVersion()}")
}
