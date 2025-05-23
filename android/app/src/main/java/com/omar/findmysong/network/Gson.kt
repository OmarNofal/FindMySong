package com.omar.findmysong.network


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.omar.findmysong.network.model.ErrorResponse
import com.omar.findmysong.network.model.SongFoundResponse
import com.omar.findmysong.network.model.SongIdentificationResponse
import java.lang.reflect.Type


val gson: Gson = GsonBuilder().registerTypeAdapter(
    SongIdentificationResponse::class.java,
    SongIdentificationResponseDeserializer()
).create()

class SongIdentificationResponseDeserializer : JsonDeserializer<SongIdentificationResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SongIdentificationResponse {

        val jsonObject = json.asJsonObject
        val result = jsonObject["result"].asString

        return when (result) {
            "success" -> context.deserialize(jsonObject, SongFoundResponse::class.java)
            "failure" -> context.deserialize(jsonObject, ErrorResponse::class.java)
            else -> throw JsonParseException("Unknown result type: $result")
        }
    }
}
