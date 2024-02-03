package qbittorrent.internal

internal actual class AtomicReference<T> actual constructor(value: T) {

    private val ref = kotlin.concurrent.AtomicReference(value)

    actual var value: T
        get() = ref.value
        set(value) {
            ref.value = value
        }
}
