package demo

import drewcarlson.qbittorrent.QBittorrentClient
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) = runBlocking {
    val client = QBittorrentClient(
        baseUrl = args.firstOrNull() ?: "http://192.168.88.6:8080"
    )

    println("qBittorrent Version: ${client.getVersion()}")

    client.syncMainData()
        .collect {
            val totalNumberOfTorrents = it.torrents.size
            val currentUploadSpeed = it.serverState.upInfoSpeed
            val currentDownloadSpeed = it.serverState.dlInfoSpeed
            val allTimeDownload = it.serverState.allTimeDownload
            val allTimeUpload = it.serverState.allTimeUpload

            println("there are $totalNumberOfTorrents torrents in total with upload speed of $currentUploadSpeed and download speed of $currentDownloadSpeed")
            println("you have all-time upload of $allTimeUpload and all-time download of $allTimeDownload")
            println()
        }
}
