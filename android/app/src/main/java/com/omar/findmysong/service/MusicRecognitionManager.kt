package com.omar.findmysong.service

import androidx.work.WorkManager
import com.omar.findmysong.OfflineRecognitionScheduler
import com.omar.findmysong.config.DEFAULT_SAMPLE_RATE
import com.omar.findmysong.config.DEFAULT_SAMPLE_SIZE
import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.network.identification.FindMySongService
import com.omar.findmysong.visualizer.BeatWithData
import com.omar.findmysong.visualizer.BeatDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File


class MusicRecognitionManager(
    cacheDir: File,
    workManager: WorkManager,
    val coroutineScope: CoroutineScope,
    val recordingService: AudioRecordService = AudioRecordService(100),
    val beatDetector: BeatDetector = BeatDetector(DEFAULT_SAMPLE_RATE, 100),
    val networkService: FindMySongService = FindMySongService(DEFAULT_SAMPLE_RATE * DEFAULT_SAMPLE_SIZE),
    val offlineRecognitionScheduler: OfflineRecognitionScheduler = OfflineRecognitionScheduler(
        cacheDir,
        workManager
    )
) : FindMySongService.Listener {

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private var isRecording = false
    private var isConnectedToServer = false

    private var timeoutJob: Job? = null

    init {
        networkService.listener = this
    }

    fun start() {
        networkService.connect()
        recordingService.startRecording(::onAudioChunkRecorded)
        isRecording = true
        startTimeout()
        emitEvent(Event.RecognitionStarted)
    }

    fun cancel() {
        if (!isRecording) return

        reset()
        emitEvent(Event.RecognitionCanceled)
    }

    private fun onAudioChunkRecorded(chunk: ByteArray) {
        if (isConnectedToServer)
            networkService.sendChunk(chunk)

        handleBeatDetection(chunk)
        offlineRecognitionScheduler.appendBuffer(chunk)
    }

    private fun handleBeatDetection(chunk: ByteArray) {
        val samples = BeatDetector.byteToFloatArray(chunk)
        val beat = beatDetector.processFrame(samples, System.currentTimeMillis())
        if (beat.isSnare || beat.isBass || beat.isKick) {
            emitEvent(Event.BeatDetected(beat))
        }
    }

    private fun startTimeout() {
        timeoutJob = coroutineScope.launch {
            delay(DEFAULT_TIMEOUT_MILLIS)
            if (isActive)
                onTimeout()
        }
    }

    private fun onTimeout() {
        if (!isConnectedToServer && isRecording) // if we are connected, then let the server handle the timeout
            scheduleForOfflineRecognition()
    }

    private fun scheduleForOfflineRecognition() {
        offlineRecognitionScheduler.schedule()
        reset()
        emitEvent(Event.ScheduledForOfflineRecognition)
    }

    private fun reset() {
        isRecording = false
        offlineRecognitionScheduler.cancel()
        networkService.stop()
        recordingService.stopRecording()

        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun emitEvent(event: Event) = coroutineScope.launch { _events.emit(event) }

    override fun onConnected() {
        isConnectedToServer = true
    }

    override fun onDisconnected() {
        isConnectedToServer = false
    }

    override fun onRecognitionTimeout() {
        reset()
        emitEvent(Event.SongNotFound)
    }

    override fun onSongFound(song: SongInfo) {
        reset()
        emitEvent(Event.SongFound(song))
    }

    override fun onSongNotFound() {
        reset()
        emitEvent(Event.SongNotFound)
    }

    sealed class Event {
        object RecognitionStarted : Event()
        object RecognitionCanceled : Event()
        object SongNotFound : Event()
        object ScheduledForOfflineRecognition : Event()
        data class BeatDetected(val beat: BeatWithData) : Event()
        data class SongFound(val song: SongInfo) : Event()
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 20_000L
    }
}