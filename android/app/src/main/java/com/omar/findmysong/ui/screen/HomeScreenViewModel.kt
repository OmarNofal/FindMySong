package com.omar.findmysong.ui.screen

import FindMySongService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.service.AudioRecordService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel @Inject constructor() : ViewModel() {

    private val audioRecord = AudioRecordService(1000)
    private val wsService = FindMySongService()
    val state = MutableStateFlow<State>(State.Idle)

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
        audioRecord.startRecording { wsService.sendChunk(it) }
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