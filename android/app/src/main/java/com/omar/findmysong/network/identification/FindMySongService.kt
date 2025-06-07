package com.omar.findmysong.network.identification

import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.network.gson
import com.omar.findmysong.network.model.ErrorResponse
import com.omar.findmysong.network.model.SongFoundResponse
import com.omar.findmysong.network.model.SongIdentificationResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import java.nio.ByteBuffer



val client = OkHttpClient()

class FindMySongService(
    bufferSize: Int
) : WebSocketListener() {

    private val request = Request.Builder().url(URL).build()
    private var ws: WebSocket? = null

    private val buffer: ByteBuffer = ByteBuffer.allocateDirect(bufferSize)

    var listener: Listener? = null

    fun connect() {
        Timber.tag("WS").d("Trying connect")
        ws = client.newWebSocket(request, this)
    }

    fun sendChunk(chunk: ByteArray) {
        var offset = 0
        while (offset < chunk.size) {
            val available = buffer.remaining()
            val toPut = minOf(available, chunk.size - offset)
            buffer.put(chunk, offset, toPut)
            offset += toPut
            if (!buffer.hasRemaining()) {
                flushBuffer()
                buffer.clear()
            }
        }
    }

    fun stop() {
        buffer.clear()
        ws?.close(1000, null)
        ws = null
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        if (text.equals("Timeout", true)) {
            listener?.onRecognitionTimeout()
            return
        }

        val response = gson.fromJson(text, SongIdentificationResponse::class.java)

        if (response is SongFoundResponse) {
            listener?.onSongFound(response.toSongModel())
        } else if (response is ErrorResponse) {
            listener?.onSongNotFound()
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {

        Timber.tag("WS").d("Connected")

        // Tell the server the type of audio data we are sending
        webSocket.send("44100")
        webSocket.send("float32")

        listener?.onConnected()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        listener?.onDisconnected()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        listener?.onDisconnected()
    }

    fun flushBuffer() {
        if (buffer.position() > 0) {
            buffer.flip() // switch to read mode
            ws?.send(buffer.toByteString())
            buffer.clear()
        }
    }

    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onRecognitionTimeout()
        fun onSongFound(song: SongInfo)
        fun onSongNotFound()
    }

    companion object {
        fun buildWsConnectionString(ip: String, port: Int) = "ws://$ip:$port/identify_song"
    }

}