package qbittorrent

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual val isWindows: Boolean
    get() = Platform.osFamily == OsFamily.WINDOWS
