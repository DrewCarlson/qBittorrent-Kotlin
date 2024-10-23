package qbittorrent.models.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object NewLineListSerializer : KSerializer<List<String>> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NewlineList", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        return decoder.decodeString().lines().filter { it.isNotEmpty() }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeString(value.joinToString("\n"))
    }
}
