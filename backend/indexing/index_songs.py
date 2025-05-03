import audiofile
import argparse
import os
from tinytag import TinyTag
from model.song import Song
from preprocessing.audio_preprocessing import preprocess_audio_file, PreprocessedAudio
from fingerprint.fingerprinting import generate_fingerprints 
from database.db import AppDatabase
from concurrent.futures import ProcessPoolExecutor, as_completed
from typing import Optional
from tqdm import tqdm
from indexing.woker import process_song

audio_file_extensions = ('mp3', 'm4a', 'flac', 'ogg', 'wav')


def index_songs_in_directory(db: AppDatabase, directory: str, max_duration: int, max_workers: int = 4):

    file_paths = []
    for root, _, file_names in os.walk(directory):
        for f in file_names:
            if f.lower().endswith(audio_file_extensions):
                file_paths.append(os.path.join(root, f))

    with ProcessPoolExecutor(max_workers=max_workers) as executor:
        futures = [executor.submit(process_song, path, max_duration) for path in file_paths]

        for future in tqdm(as_completed(futures), total=len(futures), desc="Indexing Songs"):
            result = future.result()
            if result is None:
                continue

            song, fingerprints, log_msg = result
            #print(log_msg)

            try:
                song_id = db.insert_song(song)
                db.insert_fingerprints(song_id, fingerprints)
            except Exception as e:
                print(f"DB insert failed for {song.title}: {e}")


def _extract_song_metadata(file_path: str) -> Song:
    try:
        tags = TinyTag.get(file_path)
        title = tags.title or os.path.splitext(os.path.basename(file_path))[0]
        return Song(None, title, tags.artist, tags.album, file_path, tags.duration, tags.samplerate)
    except:
        title = os.path.splitext(os.path.basename(file_path))[0]
        return Song(None, title, None, None, file_path, None, None)


if __name__ == '__main__':
    parser = argparse.ArgumentParser('Songs Database Indexing')
    parser.add_argument('dir', type=str, help='Directory to walk through to find music files')
    parser.add_argument('-max_duration', '-m', type=int, help='Max audio file duration in seconds (optional)', required=False)
    parser.add_argument('-workers', '-w', type=int, default=4, help='Number of parallel workers')
    args = parser.parse_args()

    db = AppDatabase('songs', 'postgres', 'admin')

    index_songs_in_directory(db, args.dir, args.max_duration, args.workers)

    db.close()
