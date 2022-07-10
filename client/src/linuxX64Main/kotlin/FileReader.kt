package qbittorrent

import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.*

internal actual object FileReader {
    actual fun contentOrNull(filePath: String): ByteArray? {
        val fd = fopen(filePath, "r")
        return try {
            memScoped {
                fseek(fd, 0, SEEK_END)
                val size = ftell(fd).convert<Int>()
                fseek(fd, 0, SEEK_SET)

                return ByteArray(size + 1).also { buffer ->
                    fread(buffer.refTo(0), 1UL, size.convert(), fd)
                        .ensureUnixCallResult("fread") { ret -> ret > 0U }
                }
            }
        } catch (e: Throwable) {
            null
        } finally {
            fclose(fd).ensureUnixCallResult("fclose") { ret -> ret == 0 }
        }
    }
}

private inline fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean): Int {
    if (!predicate(this)) {
        error("$op: ${checkNotNull(strerror(posix_errno())).toKString()}")
    }
    return this
}

private inline fun ULong.ensureUnixCallResult(op: String, predicate: (ULong) -> Boolean): ULong {
    if (!predicate(this)) {
        error("$op: ${checkNotNull(strerror(posix_errno())).toKString()}")
    }
    return this
}
