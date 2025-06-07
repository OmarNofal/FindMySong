package com.omar.findmysong.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.toBitmap
import com.omar.findmysong.AppNotificationManager
import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.network.gson
import com.omar.findmysong.network.identification.Api
import com.omar.findmysong.network.model.ErrorResponse
import com.omar.findmysong.network.model.SongFoundResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File


class OfflineRecognizerWorker(
    val appContext: Context, params: WorkerParameters
) : CoroutineWorker(appContext, params) {


    private val retrofit =
        Retrofit.Builder().baseUrl("http://192.168.1.77:8000").addConverterFactory(
            GsonConverterFactory.create(gson)
        ).build()

    override suspend fun doWork(): Result {

        Timber.tag("Worker").d("Worker started")
        val recordingFile = File(appContext.externalCacheDir!!, "temp")

        if (!recordingFile.exists()) return Result.success()


        val api = retrofit.create(Api::class.java)

        val requestFile = recordingFile.asRequestBody("application/octet-stream".toMediaType())
        val body = MultipartBody.Part.createFormData("file", recordingFile.name, requestFile)


        val result = withContext(
            Dispatchers.IO
        ) {
            api.matchSongOneShot(body, 44100, "float32")
        }
        if (result is SongFoundResponse) {

            val songId = result.id
            val albumArt = getAlbumArt(songId)

            val song = result.toSongModel()
            sendNotification(song, albumArt)

        } else if (result is ErrorResponse) {
            sendNotFoundNotification()
        }


        recordingFile.delete()
        return Result.success()
    }

    private fun sendNotification(song: SongInfo, albumArt: Bitmap?) =
        AppNotificationManager.get().notifySongIdentifiedOffline(song, albumArt)

    private fun sendNotFoundNotification() =
        AppNotificationManager.get().notifySongNotIdentifiedOffline()

    private suspend fun getAlbumArt(songId: Int) = withContext(Dispatchers.IO) {

        val imageLoader = appContext.imageLoader

        val imageRequest = ImageRequest.Builder(appContext)
            .data("http://192.168.1.77:8000/get_albumart?song_id=$songId")
            .build()

        val result: ImageResult = imageLoader.execute(imageRequest)

        if (result is SuccessResult) {
            val image = result.image
            image.toBitmap()
        } else {
            null
        }
    }
}