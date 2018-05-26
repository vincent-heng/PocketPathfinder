package fr.project24.fox.pocketpathfinder.retrofit

import com.google.gson.*
import org.joda.time.DateTime
import java.lang.reflect.Type

internal class DateTimeTypeConverter : JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
    override fun serialize(src: DateTime, srcType: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): DateTime {
        return DateTime(json.asString)
    }
}