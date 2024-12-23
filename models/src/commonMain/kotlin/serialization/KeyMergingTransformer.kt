package qbittorrent.models.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import qbittorrent.models.Torrent

internal object KeyMergingTransformer : JsonTransformingSerializer<Map<String, Torrent>>(
    MapSerializer(String.serializer(), Torrent.serializer())
) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        return element
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject
            .mapValues { (key, value) ->
                value.jsonObject
                    .toMutableMap()
                    .apply { put("hash", JsonPrimitive(key)) }
                    .run(::JsonObject)
            }
            .run(::JsonObject)
    }
}
