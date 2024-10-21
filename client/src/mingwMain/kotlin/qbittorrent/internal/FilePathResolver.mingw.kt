package qbittorrent.internal

import kotlinx.cinterop.*
import kotlinx.io.files.SystemPathSeparator
import platform.windows.FOLDERID_Profile
import platform.windows.PWSTRVar
import platform.windows.SHGetKnownFolderPath

internal actual object FilePathResolver {
    actual fun resolve(filePath: String): String {
        return if (filePath.startsWith("%USERPROFILE%", true)) {
            val userProfile: String = memScoped {
                val out = alloc<PWSTRVar>()
                if (SHGetKnownFolderPath(FOLDERID_Profile.ptr, 0u, null, out.ptr) != 0) {
                    return filePath
                }
                out.value?.toKString() ?: "%USERPROFILE%"
            }
            filePath.replace("%USERPROFILE%", userProfile.trimEnd(SystemPathSeparator), true)
        } else {
            filePath
        }
    }
}