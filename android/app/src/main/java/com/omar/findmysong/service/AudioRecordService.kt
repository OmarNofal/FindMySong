package com.omar.findmysong.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.math.ceil


class AudioRecordService(private val preferredChunkSizeMs: Int) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val AUDIO_SOURCE = AudioSource.MIC
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val SAMPLE_RATE_IN = 44100
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT

    val state = MutableStateFlow(State.READY)

    fun startRecording(onChunkReady: (ByteArray) -> Unit) {
        val minBufferSize =
            AudioRecord.getMinBufferSize(SAMPLE_RATE_IN, CHANNEL_CONFIG, AUDIO_FORMAT)

        try {
            audioRecord =
                AudioRecord(
                    AUDIO_SOURCE,
                    SAMPLE_RATE_IN,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    minBufferSize
                )
            state.value = State.RECORDING

            audioRecord!!.startRecording()

            recordingJob = CoroutineScope(Dispatchers.IO).launch {

                val bytesPerSample = 4
                val bufferSize =
                    (bytesPerSample * ceil(preferredChunkSizeMs / 1000.0) * SAMPLE_RATE_IN).toInt()
                val buffer = ByteBuffer.allocateDirect(bufferSize)

                while (isActive) {
                    val read = audioRecord!!.read(buffer, bufferSize)
                    Timber.tag("WS").d("Read $read bytes from mic")
                    if (read > 0) {
                        withContext(Dispatchers.Main) {
                            val byteArray = ByteArray(read)
                            buffer.get(byteArray, 0, read)
                            onChunkReady(byteArray)
                        }
                        buffer.clear()
                    }
                }
                audioRecord!!.stop()
                audioRecord!!.release()
                audioRecord = null
            }

        } catch (e: SecurityException) {
            Log.e("Permission", "MIC permission not granted")
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        state.value = State.READY
    }

    enum class State {
        READY, RECORDING
    }

}