package qbittorrent

import kotlinx.cinterop.*
import platform.windows.*

internal actual object FileReader {
    actual fun contentOrNull(filePath: String): ByteArray? {
        val handle = CreateFileA(
            filePath,
            GENERIC_READ,
            FILE_SHARE_READ,
            null,
            OPEN_EXISTING,
            0,
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
