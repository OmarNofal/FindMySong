import pprint
from time import time
import fastapi
from fastapi import File, HTTPException, Response, UploadFile, WebSocket
from fastapi.responses import JSONResponse
import numpy as np
from tinytag import TinyTag
from config.constants import DEFAULT_SAMPLE_RATE
from api.song_id_session import SessionConfiguration, SongIdSession
from database.db import AppDatabase
from fingerprint.fingerprinting import generate_fingerprints
from model.song import Song
from preprocessing.audio_preprocessing import PreprocessedAudio
from matching.matching import get_audio_matches
from starlette.websockets import WebSocketDisconnect
from scipy.signal import resample

app = fastapi.FastAPI()

db = AppDatabase('songs', 'postgres', 'admin')

@app.websocket('/identify_song')
async def identify_song(ws: WebSocket):
    print(f"{ws.client.host} Connected")
    await ws.accept()

    in_sample_rate = int(await ws.receive_text())
    dtype = await ws.receive_text()

    print(in_sample_rate, dtype)
    config = SessionConfiguration(in_sample_rate, DEFAULT_SAMPLE_RATE, dtype, 3, 1000, 300)
    session = SongIdSession(db=db, config=config)
    
    time_s = 0

    while True:

        c_time = time()
        try:
            data = await ws.receive_bytes()
            session.push_bytes(data)
        except WebSocketDisconnect:
            print("User disconnected early")
            break
        time_s += time() - c_time

        if session.is_match_found:
                
            results = sorted(session.results.items(), key=lambda x: x[1], reverse=True)
            top_song_id = results[0][0]
            top_song = db.get_song(top_song_id)
            
            res = prepare_sucess_result(top_song)
            try:
                pprint.pprint(res)
                await ws.send_json(res)
                await ws.close()
            except RuntimeError as e:
                print(e.__cause__)

        if time_s > 20:
            print("Timeout")
            res = prepare_failure_result()
            await ws.send_json(res)
            await ws.close()
            break

@app.post('/recognize_song_one_shot')
async def recognize_song_one_shot(
    file: UploadFile,
    sample_rate: int,
    dtype: str
):
    
    contents = await file.read()
    
    signal = np.frombuffer(contents, dtype=dtype)
    duration_sec = len(signal) / sample_rate

    if sample_rate != DEFAULT_SAMPLE_RATE:
        num_samples = int(duration_sec * DEFAULT_SAMPLE_RATE)
        signal = resample(signal, num_samples)

    preprocessed = PreprocessedAudio(signal, DEFAULT_SAMPLE_RATE, duration_sec)
    result = get_audio_matches(db, preprocessed, 1)

    song_id = result[0][0]
    if song_id is None or result[0][1] < 20:
        res = prepare_failure_result()
        return JSONResponse(res)

    song = db.get_song(song_id)
    res = prepare_sucess_result(song)

    return JSONResponse(res)



@app.get('/get_albumart')
def get_albumart(song_id: int):

    song = db.get_song(song_id)
    if song is None:
        raise HTTPException(status_code=404, detail='Invalid song id')

    song_path = song.file_path
    tags = TinyTag.get(song_path, image=True)
    pic = tags.images.front_cover

    if pic is None:
        raise HTTPException(status_code=404, detail='No album art')

    return Response(media_type=pic.mime_type, content=pic.data)

@app.get('/get_albumart')
def get_albumart(song_id: int):

    song = db.get_song(song_id)
    if song is None:
        raise HTTPException(status_code=404, detail='Invalid song id')

    song_path = song.file_path
    tags = TinyTag.get(song_path, image=True)
    pic = tags.images.front_cover

    if pic is None:
        raise HTTPException(status_code=404, detail='No album art')

    return Response(media_type=pic.mime_type, content=pic.data)


def prepare_sucess_result(song: Song):
    return {
        'result': 'success',
        'id': song.id,
        'title': song.title,
        'artist': song.artist_name,
        'album': song.album_name
    }

def prepare_failure_result():
    return {
        'result': 'failure',
        'reason': 'timeout'
    }