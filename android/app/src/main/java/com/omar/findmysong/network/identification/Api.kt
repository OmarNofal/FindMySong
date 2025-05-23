package com.omar.findmysong.network.identification

import com.omar.findmysong.network.model.SongIdentificationResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query


interface Api {

    @POST("/recognize_song_one_shot")
    @Multipart
    suspend fun matchSongOneShot(
        @Part file: MultipartBody.Part,
        @Query("sample_rate") sampleRate: Int,
        @Query("dtype") dtype: String
    ): SongIdentificationResponse

}