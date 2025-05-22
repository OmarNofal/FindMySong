package com.omar.findmysong.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
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
        val bytesPerSample = 4 // float32 = 4 bytes
        val chunkDurationMs = 100
        val chunkBufferSize = (bytesPerSample * SAMPLE_RATE_IN * (chunkDurationMs / 1000.0)).toInt()
        val minBufferSize =
            AudioRecord.getMinBufferSize(SAMPLE_RATE_IN, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = chunkBufferSize

        try {
            audioRecord = AudioRecord(
                AUDIO_SOURCE,
                SAMPLE_RATE_IN,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Timber.e("AudioRecord initialization failed")
                state.value = State.READY
                return
            }

            audioRecord!!.startRecording()
            state.value = State.RECORDING

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteBuffer.allocateDirect(bufferSize)

                try {
                    while (isActive) {
                        val read = audioRecord!!.read(buffer, bufferSize)

                        if (read > 0) {
                            val byteArray =
                                ByteArray(buffer.remaining()) // remaining() == number of bytes available
                            buffer.get(byteArray)

                            withContext(Dispatchers.Main) {
                                onChunkReady(byteArray)
                                buffer.clear() // ready for next write
                            }
                        }
                    }
                } finally {
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                    state.value = State.READY
                }
            }
        } catch (e: SecurityException) {
            Timber.e("Microphone permission denied: ${e.message}")
            state.value = State.READY
            // Optionally, notify the caller about permission failure via another flow/state or callback
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