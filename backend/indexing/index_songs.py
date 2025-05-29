from multiprocessing import Queue
import audiofile
import argparse
import os
from prettytable import PrettyTable
from termcolor import colored
from tinytag import TinyTag
from database.config import DB_NAME, DB_PASS, DB_USER
from indexing.config import IndexConfig
from indexing.index_output import _print_failed_songs, _print_success_songs
from indexing.index_process import IndexProcessOptions, IndexProcess
from indexing.index_result import SongIndexError, SongIndexSuccess
from model.song import Song
from preprocessing.audio_preprocessing import preprocess_audio_file, PreprocessedAudio
from fingerprint.fingerprinting import generate_fingerprints 
from database.db import AppDatabase
from concurrent.futures import ProcessPoolExecutor, as_completed
from typing import List, Optional
from tqdm import tqdm
from pprint import pprint

audio_file_extensions = ('mp3', 'm4a', 'flac', 'ogg', 'wav')

def create_db_connection():
    return AppDatabase('songs', 'postgres', 'admin')

def index_songs_in_directory(directory: str, config: IndexConfig):

    file_paths = _get_all_candidate_files(directory)
    

    total_files = len(file_paths)

    task_queue = Queue()
    results_queue = Queue()
    

    # Feed all tasks to queue
    for path in file_paths:
        task_queue.put(path)

    
    success_result = []
    error_results = []

    # Create and start workers
    workers = []
    options = IndexProcessOptions(max_duration_sec=config.max_duration_sec)
    for _ in range(config.num_workers):
        worker = IndexProcess(
            task_queue=task_queue,
            progress_queue=results_queue,
            options=options,
            db_factory=create_db_connection
        )
        worker.start()
        workers.append(worker)

    # Progress loop
    with tqdm(total=total_files, desc="Indexing Songs", unit="song") as pbar:
        completed = 0
        while completed < total_files:
            result = results_queue.get()  # waits for signal from any worker
            if isinstance(result, SongIndexSuccess):
                success_result.append(result)
            elif isinstance(result, SongIndexError):
                error_results.append(result)

            completed += 1
            pbar.update(1)

    for worker in workers:
        worker.join()


    print(colored("\nIndexing complete", color='blue', attrs=['bold','underline']))
    print(f"✅ Successful: {len(success_result)} songs")
    print(f"❌ Failed:     {len(error_results)} songs")
    print()

    if config.print_tables:
        _print_success_songs(success_result)
        print()
        _print_failed_songs(error_results)
    


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
    parser.add_argument('--max_duration', '-m', type=int, help='Max audio file duration in seconds (optional)', required=False)
    parser.add_argument('--workers', '-w', type=int, default=4, help='Number of parallel workers')
    parser.add_argument('--print-table', '-pt', action='store_true', help='Prints tables containing the results')
    args = parser.parse_args()

    db = AppDatabase(DB_NAME, DB_USER, DB_PASS)
    db.create_tables()

    config = IndexConfig(num_workers=args.workers, max_duration_sec=args.max_duration, print_tables=args.print_table)
    index_songs_in_directory(args.dir, config)

    db.close()

