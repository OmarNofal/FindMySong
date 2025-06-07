package com.omar.findmysong.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.omar.findmysong.di.TempDirectory
import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.network.discovery.ServerDiscovery
import com.omar.findmysong.service.MusicRecognitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    @TempDirectory val cacheDir: File,
    workManager: WorkManager,
    serverDiscovery: ServerDiscovery
) : ViewModel() {


    val recognitionManager = MusicRecognitionManager(
        cacheDir, workManager, CoroutineScope(
            Dispatchers.Default
        ),
        serverDiscovery = serverDiscovery
    )

    val state = MutableStateFlow<State>(State.Idle)

    private val _beatsFlow = MutableSharedFlow<Unit>()
    val beatsFlow = _beatsFlow.asSharedFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            recognitionManager.events.collect { event ->
                handleManagerEvent(event)
            }
        }
    }

    private fun handleManagerEvent(event: MusicRecognitionManager.Event) {
        when (event) {
            is MusicRecognitionManager.Event.RecognitionStarted -> onRecognitionStarted()
            is MusicRecognitionManager.Event.RecognitionCanceled -> onRecognitionCanceled()
            is MusicRecognitionManager.Event.BeatDetected -> onBeatDetected()
            is MusicRecognitionManager.Event.ScheduledForOfflineRecognition -> onScheduledForOfflineRecognition()
            is MusicRecognitionManager.Event.SongFound -> onSongFound(event.song)
            is MusicRecognitionManager.Event.SongNotFound -> onSongNotFound()
        }
    }

    fun start() {
        recognitionManager.start()
    }

    fun stop() {
        if (state.value is State.Found)
            state.value = State.Idle
        else
            recognitionManager.cancel()
    }

    private fun onSongFound(song: SongInfo) {
        state.value = State.Found(song)
    }

    private fun onSongNotFound() {
        state.value = State.Idle
        emitEvent(Event.SongNotFound)
    }

    private fun onScheduledForOfflineRecognition() {
        state.value = State.Idle
        emitEvent(Event.ScheduledForOfflineRecognition)
    }

    private fun onBeatDetected() {
        viewModelScope.launch { _beatsFlow.emit(Unit) }
    }

    private fun onRecognitionCanceled() {
        state.value = State.Idle
    }

    private fun onRecognitionStarted() {
        state.value = State.Identifying
    }

    private fun emitEvent(event: Event) {
        viewModelScope.launch { _events.emit(event) }
    }

    sealed class State {
        object Idle : State()
        object Identifying : State()
        class Found(val songInfo: SongInfo) : State()
    }

    sealed class Event {
        object SongNotFound : Event()
        object ScheduledForOfflineRecognition : Event()
    }

}