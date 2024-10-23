package qbittorrent.models

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1

class QBittorrentPrefsBuilder {
    private val prefs = mutableMapOf<String, JsonElement>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> set(property: KProperty1<QBittorrentPrefs, T>, value: T?) {
        val serialName = QBittorrentPrefsSerialNameMap.map(property.name)
        prefs[serialName] = when {
            value == null -> JsonNull
            else -> Json.encodeToJsonElement(property.getSerializer() as KSerializer<Any>, value)
        }
    }

    @JvmName("setInfix")
    infix fun <T : Any> KProperty1<QBittorrentPrefs, T>.set(value: T?) {
        set(this, value)
    }

    fun build(): JsonObject {
        return Json.encodeToJsonElement(prefs) as JsonObject
    }
}
