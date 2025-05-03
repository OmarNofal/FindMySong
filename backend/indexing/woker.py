from preprocessing.audio_preprocessing import preprocess_audio_file
from fingerprint.fingerprinting import generate_fingerprints
from model.song import Song
from tinytag import TinyTag
import audiofile
import os

def process_song(file_path: str, max_duration: int):
    try:
        duration = audiofile.duration(file_path)
        if max_duration and duration > 0 and duration > max_duration:
            return None

        sample_rate = audiofile.sampling_rate(file_path)
        channels = audiofile.channels(file_path)
        log_msg = f"Processing: {file_path}\nDuration: {duration:.2f}s, Sample rate: {sample_rate}, Channels: {channels}"

        preprocessed_audio = preprocess_audio_file(file_path)
        fingerprints = generate_fingerprints(preprocessed_audio, window_size=2048, hop_size=512)

        tags = TinyTag.get(file_path)
        title = tags.title or os.path.splitext(os.path.basename(file_path))[0]
        song = Song(None, title, tags.artist, tags.album, file_path, preprocessed_audio.duration_seconds, preprocessed_audio.rate)

        return song, fingerprints, log_msg

    except Exception as e:
        return None
