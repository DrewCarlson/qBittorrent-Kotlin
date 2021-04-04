package demo

import drewcarlson.qbittorrent.QBittorrentClient
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) = runBlocking {
    val client = QBittorrentClient(
        baseUrl = args.firstOrNull() ?: "http://localhost:9090"
    )

    println("qBittorrent Version: ${client.getVersion()}")
    client.syncMainData()
        .collect { mainData ->
            println("\n")
            mainData.toString()
                .split("(", ")", ", ")
                .joinToString("\n")
                .run(::println)
        }
}
