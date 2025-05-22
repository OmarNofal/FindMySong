package com.omar.findmysong.ui.screen

import FindMySongService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.service.AudioRecordService
import com.omar.findmysong.visualizer.BassBeatDetector
import com.omar.findmysong.visualizer.BeatDetector
import com.omar.findmysong.visualizer.SpectralFluxDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel @Inject constructor() : ViewModel() {

    private val audioRecord = AudioRecordService(50)
    private val beatDetector = BassBeatDetector(44100, 100)
    private val wsService = FindMySongService(44100 * 4)
    val state = MutableStateFlow<State>(State.Idle)

    private val _beatsFlow = MutableSharedFlow<Unit>()
    val beatsFlow = _beatsFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            wsService.state.collect { wsState ->
                when (wsState) {
                    is FindMySongService.State.Found ->
                        onSongFound(wsState.response.toSongModel())

                    is FindMySongService.State.NotFound ->
                        onSongNotFound()

                    is FindMySongService.State.Error -> onSongNotFound()
                    else -> {}
                }
            }
        }
    }

    fun start() {
        state.value = State.Identifying
        wsService.connect()
        audioRecord.startRecording { it ->
            Timber.e("Read ${it.size} bytes")
            wsService.sendChunk(it)
            val isBeat = beatDetector.processFrame(
                BeatDetector.byteToFloatArray(it),
                System.currentTimeMillis()
            )
            if (isBeat) {
                viewModelScope.launch {
                    _beatsFlow.emit(Unit)
                }
            }
        }
    }

    fun stop() {
        wsService.stop()
        audioRecord.stopRecording()
        state.value = State.Idle
    }

    fun cancel() {
        state.value = State.Idle
    }

    private fun onSongFound(songInfo: SongInfo) {
        state.value = State.Found(songInfo)
        audioRecord.stopRecording()
    }

    private fun onSongNotFound() {
        state.value = State.NotFound()
        audioRecord.stopRecording()
    }

    sealed class State {
        object Idle : State()
        object Identifying : State()
        class Found(val songInfo: SongInfo) : State()
        class NotFound : State()
    }

}