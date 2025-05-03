import fastapi
from fastapi import WebSocket
import numpy as np
from database.db import AppDatabase
from fingerprint.fingerprinting import generate_fingerprints
from preprocessing.audio_preprocessing import PreprocessedAudio
from matching.matching import get_audio_matches

app = fastapi.FastAPI()

db = AppDatabase('songs', 'postgres', 'admin')

@app.websocket('/identify_song')
async def identify_song(ws: WebSocket):
    await ws.accept()

    # assuming audio is 44.1kHz mono PCM 
    audio_buffer = bytearray()
    while True:
        data = await ws.receive_bytes()
        audio_buffer.extend(data)

        min_samples_time = 5 # sec
        min_samples_len = (min_samples_time * 44100 * 4)

        if len(audio_buffer) >= min_samples_len:
            # Generate fingerprints
            audio_bytes = bytes(audio_buffer)
            audio_signal = np.frombuffer(audio_bytes, dtype=np.float32) 
            
            preprocessed_audio = PreprocessedAudio(signal=audio_signal, rate=44100, duration_seconds=len(audio_signal) / 44100)
            matches = get_audio_matches(db, preprocessed_audio)

            print(matches[:5])
            audio_buffer = bytearray()

