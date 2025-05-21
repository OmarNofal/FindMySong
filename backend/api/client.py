import sounddevice as sd
import numpy as np
import asyncio
import websockets
import threading
import queue
import signal

from config.constants import DEFAULT_SAMPLE_RATE

# === Config ===
SAMPLE_RATE = DEFAULT_SAMPLE_RATE
CHUNK_DURATION = 1
BLOCK_SIZE = int(SAMPLE_RATE * CHUNK_DURATION)
CHANNELS = 1
SERVER_URI = 'ws://localhost:8000/identify_song'

audio_queue = queue.Queue()
stop_event = threading.Event()

def audio_callback(indata, frames, time, status):
    if status:
        print("[Audio status]", status)
    chunk = indata[:, 0].copy()
    audio_queue.put(chunk)

def normalize_audio(chunk):
    max_val = np.max(np.abs(chunk))
    return chunk / max_val if max_val != 0 else chunk
    
def start_audio_stream():
    def runner():
        with sd.InputStream(callback=audio_callback,
                            samplerate=SAMPLE_RATE,
                            blocksize=BLOCK_SIZE,
                            channels=CHANNELS,
                            dtype='float32',
                            device=8
                            ):
            print("[Audio] Recording started...")
            stop_event.wait()
        print("[Audio] Recording stopped.")
    threading.Thread(target=runner, daemon=True).start()

async def stream_to_server():
    print("[WebSocket] Connecting...")
    async with websockets.connect(SERVER_URI) as ws:
        print("[WebSocket] Connected.")
        await ws.send(str(SAMPLE_RATE))
        await ws.send('float32')
        while not stop_event.is_set():
            try:
                chunk = await asyncio.get_event_loop().run_in_executor(None, audio_queue.get)
                norm = normalize_audio(chunk)
                await ws.send(norm.astype(np.float32).tobytes())
            except Exception as e:
                print("[Error] Streaming:", e)
                break

def shutdown():
    print("[System] Shutting down...")
    stop_event.set()

def main():
    # Ensure clean Ctrl+C handling
    start_audio_stream()
    asyncio.run(stream_to_server())

if __name__ == "__main__":
    print(sd.query_devices())
    main()
