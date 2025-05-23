package com.omar.findmysong.network.model

import androidx.annotation.Keep
import com.omar.findmysong.model.SongInfo
import okhttp3.ResponseBody
import retrofit2.Response
import java.lang.Error


@Keep
open class SongIdentificationResponse(
    val status: String
)

@Keep
class SongFoundResponse(
    status: String,
    val id: Int,
    val album: String,
    val artist: String,
    val title: String
) : SongIdentificationResponse(status) {

    fun toSongModel() = SongInfo(id, title, album, artist)
}

@Keep
class ErrorResponse(
    status: String,
    val reason: String
) : SongIdentificationResponse(status)