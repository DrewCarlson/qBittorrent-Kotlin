package qbittorrent.models.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import qbittorrent.models.preferences.ScanDir

object ScanDirSerializer : KSerializer<List<ScanDir>> {
    private val mapSerializer = MapSerializer(String.serializer(), AnyScanDirSerializer())
    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: List<ScanDir>) {
        val map = value.associate { scanDir ->
            when (scanDir) {
                is ScanDir.MonitoredFolder -> scanDir.scanDir to 0
                is ScanDir.DefaultSavePath -> scanDir.scanDir to 1
                is ScanDir.CustomSavePath -> scanDir.scanDir to scanDir.path
            }
        }
        mapSerializer.serialize(encoder, map)
    }

    override fun deserialize(decoder: Decoder): List<ScanDir> {
        return mapSerializer.deserialize(decoder).map { (key, value) ->
            when (value) {
                is Int -> when (value) {
                    0 -> ScanDir.MonitoredFolder(key)
                    1 -> ScanDir.DefaultSavePath(key)
                    else -> throw SerializationException("Unknown value for ScanDir: $value")
                }
                is String -> ScanDir.CustomSavePath(key, value)
                else -> throw SerializationException("Unknown value for ScanDir: $value")
            }
        }
    }
}

private class AnyScanDirSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AnyScanDir", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is Int -> encoder.encodeInt(value)
            is String -> encoder.encodeString(value)
            else -> throw SerializationException("Unsupported type for ScanDir: ${value::class}")
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        return when (val input = decoder.decodeString()) {
            "0", "1" -> input.toInt()
            else -> input
        }
    }
}
