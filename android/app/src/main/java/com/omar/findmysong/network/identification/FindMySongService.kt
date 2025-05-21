import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.omar.findmysong.network.SongIdentificationResponseDeserializer
import com.omar.findmysong.network.model.ErrorResponse
import com.omar.findmysong.network.model.SongFoundResponse
import com.omar.findmysong.network.model.SongIdentificationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber



const val URL = "ws://192.168.1.77:8000/identify_song"
val client = OkHttpClient()

class FindMySongService : WebSocketListener() {

    private val request = Request.Builder().url(URL).build()
    private var ws: WebSocket? = null

    var gson: Gson = GsonBuilder().registerTypeAdapter(
        SongIdentificationResponse::class.java,
        SongIdentificationResponseDeserializer()
    ).create()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun connect() {
        Timber.tag("WS").d("Trying connect")
        ws = client.newWebSocket(request, this)
    }

    fun sendChunk(chunk: ByteArray) {
        ws?.send(ByteString.of(*chunk))
    }

    fun stop() {
        ws?.close(1000, "")
        ws = null
        _state.value = State.Idle
    }

    override fun onMessage(webSocket: WebSocket, text: String) {

        val response = gson.fromJson(text, SongIdentificationResponse::class.java)

        if (response is SongFoundResponse) {
            _state.value = State.Found(response)
        } else if (response is ErrorResponse) {
            _state.value = State.NotFound(response)
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {

    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        // 1. Send Audio Information
        Timber.tag("WS").d("Connected")
        webSocket.send("44100")
        webSocket.send("float32")
        _state.value = State.Connected
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        _state.value = State.Idle
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        _state.value = State.Error(t)
    }

    sealed class State {
        object Idle : State()
        object Connected : State()
        class Error(val t: Throwable) : State()
        class NotFound(val response: ErrorResponse) : State()
        class Found(val response: SongFoundResponse) : State()
    }

}