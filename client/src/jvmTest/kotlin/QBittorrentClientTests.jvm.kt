package qbittorrent

actual val isWindows: Boolean
    get() = System.getProperty("os.name").contains("win", ignoreCase = true)
