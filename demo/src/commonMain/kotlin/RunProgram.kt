package demo

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.flow.*
import qbittorrent.*

suspend fun runProgram(args: Array<out String>) {
    val client = QBittorrentClient(
        baseUrl = args.firstOrNull() ?: "http://localhost:9090",
        httpClient = HttpClient {
            Logging {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
            }
        }
    )

    println("qBittorrent Version: ${client.getVersion()}")
    client.observeMainData()
        .onEach { mainData ->
            println("\n")
            mainData.toString()
                .split("(", ")", ", ")
                .joinToString("\n")
                .run(::println)
        }
        .collect()
}
