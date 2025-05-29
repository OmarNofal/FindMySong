from multiprocessing import Queue
import audiofile
import argparse
import os
from tinytag import TinyTag
from database.config import DB_NAME, DB_PASS, DB_USER
from indexing.index_process import IndexOptions, IndexProcess, IndexProcessResult
from model.song import Song
from preprocessing.audio_preprocessing import preprocess_audio_file, PreprocessedAudio
from fingerprint.fingerprinting import generate_fingerprints 
from database.db import AppDatabase
from concurrent.futures import ProcessPoolExecutor, as_completed
from typing import Optional
from tqdm import tqdm

audio_file_extensions = ('mp3', 'm4a', 'flac', 'ogg', 'wav')

def create_db_connection():
    return AppDatabase('songs', 'postgres', 'admin')

def index_songs_in_directory(directory: str, max_duration: int, max_workers: int = 4):

    file_paths = _get_all_candidate_files(directory)
    

    total_files = len(file_paths)

    task_queue = Queue()
    progress_queue = Queue()
    results_queue = Queue()

    # Feed all tasks to queue
    for path in file_paths:
        task_queue.put(path)

   
    # Create and start workers
    workers = []
    options = IndexOptions(max_duration_sec=max_duration)
    for _ in range(max_workers):
        worker = IndexProcess(
            task_queue=task_queue,
            progress_queue=progress_queue,
            results_queue=results_queue,
            options=options,
            db_factory=create_db_connection
        )
        worker.start()
        workers.append(worker)

    # Progress loop
    with tqdm(total=total_files, desc="Indexing Songs", unit="song") as pbar:
        completed = 0
        while completed < total_files:
            progress_queue.get()  # waits for signal from any worker
            completed += 1
            pbar.update(1)

    for worker in workers:
        worker.join()

    result = IndexProcessResult()
    for _ in range(results_queue.qsize()):
        result += results_queue.get()

    print("\nIndexing complete")
    print("-----------------\n")
    print(f"✅ Successful: {len(result.finished)} songs")
    print(f"❌ Failed:     {len(result.failed)} songs")
    
    if len(result.failed) > 0:
        print(f"\nFailed songs: ")
        for f in result.failed:
            print(f"\t{os.path.basename(f)}")

def _get_all_candidate_files(directory):
    file_paths = []
    for root, _, file_names in os.walk(directory):
        for f in file_names:
            if f.lower().endswith(audio_file_extensions):
                file_paths.append(os.path.join(root, f))
    return file_paths

if __name__ == '__main__':
    parser = argparse.ArgumentParser('Songs Database Indexing')
    parser.add_argument('dir', type=str, help='Directory to walk through to find music files')
    parser.add_argument('-max_duration', '-m', type=int, help='Max audio file duration in seconds (optional)', required=False)
    parser.add_argument('-workers', '-w', type=int, default=4, help='Number of parallel workers')
    args = parser.parse_args()

    db = AppDatabase(DB_NAME, DB_USER, DB_PASS)
    db.create_tables()

    index_songs_in_directory(args.dir, args.max_duration, args.workers)

    db.close()

