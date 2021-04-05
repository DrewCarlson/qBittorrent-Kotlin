package drewcarlson.qbittorrent

import kotlin.native.concurrent.freeze

internal actual class AtomicReference<T> actual constructor(value: T) {

    private val ref = kotlin.native.concurrent.AtomicReference(value.freeze())

    actual var value: T
        get() = ref.value
        set(value) {
            ref.value = value.freeze()
        }
}
