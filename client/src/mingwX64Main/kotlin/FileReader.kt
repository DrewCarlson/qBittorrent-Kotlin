package qbittorrent.internal

import kotlinx.cinterop.*
import platform.windows.*

internal actual object FileReader {
    actual fun contentOrNull(filePath: String): ByteArray? {
        val actualPath: String = if (filePath.startsWith("%USERPROFILE%", true)) {
            val userProfile: String = memScoped {
                val out = alloc<PWSTRVar>()
                if (SHGetKnownFolderPath(FOLDERID_Profile.ptr, 0u, null, out.ptr) != 0) {
                    return null
                }
                out.value?.toKString().orEmpty()
            }
            filePath.replace("%USERPROFILE%", userProfile, true)
        } else {
            filePath
        }
        val attrs = GetFileAttributesA(actualPath)
        if (attrs == INVALID_FILE_ATTRIBUTES) return null

        val handle = CreateFileA(
            actualPath,
            GENERIC_READ,
            FILE_SHARE_READ.convert(),
            null,
            OPEN_EXISTING.convert(),
            0u,
            null
        )
        if (handle == INVALID_HANDLE_VALUE) return null

        return try {
            memScoped {
                val fs = alloc<_LARGE_INTEGER>()
                if (GetFileSizeEx(handle, fs.ptr) == TRUE) {
                    val size = (fs.HighPart.toUInt() shl 32) or fs.LowPart
                    val buf = allocArray<ByteVar>(size.toInt())
                    val bytesRead = alloc<UIntVar>()
                    if (ReadFile(handle, buf, size, bytesRead.ptr, null) == TRUE) {
                        buf.readBytes(bytesRead.value.toInt())
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Throwable) {
            null
        } finally {
            CloseHandle(handle)
        }
    }
}
