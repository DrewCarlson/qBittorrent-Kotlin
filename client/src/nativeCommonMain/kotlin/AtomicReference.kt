package qbittorrent.internal

import kotlin.native.concurrent.freeze

internal actual class AtomicReference<T> actual constructor(value: T) {

    private val ref = kotlin.native.concurrent.AtomicReference(value.maybeFreeze())

    actual var value: T
        get() = ref.value
        set(value) {
            ref.value = value.maybeFreeze()
        }
}

private fun <T> T.maybeFreeze(): T {
    return if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) this else freeze()
}
