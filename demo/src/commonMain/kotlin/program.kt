package demo

import qbittorrent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun runProgram(args: Array<out String>) = runBlocking {
    val client = QBittorrentClient(
        baseUrl = args.firstOrNull() ?: "http://localhost:9090"
    )

    println("qBittorrent Version: ${client.getVersion()}")
    client.syncMainData()
        .onEach { mainData ->
            println("\n")
            mainData.toString()
                .split("(", ")", ", ")
                .joinToString("\n")
                .run(::println)
        }
        .collect()
}
