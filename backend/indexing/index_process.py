
from dataclasses import dataclass, field
from multiprocessing import Process, Queue
import os
import queue
from typing import Callable, List

import audiofile
from tinytag import TinyTag

from config.constants import HOP_SIZE, WINDOW_SIZE
from database.db import AppDatabase
from fingerprint.fingerprinting import generate_fingerprints
from model.song import Song
from preprocessing.audio_preprocessing import PreprocessedAudio, preprocess_audio_file



@dataclass
class IndexOptions:
    max_duration_sec: int

@dataclass
class Tags:
    title: str
    artist: str
    album: str

@dataclass
class IndexProcessResult:
    finished: List[str] = field(default_factory=lambda: [])
    failed: List[str] = field(default_factory=lambda: [])

    def __add__(self, other: "IndexProcessResult") -> "IndexProcessResult":
        return IndexProcessResult(
            finished=self.finished + other.finished,
            failed=self.failed + other.failed
        )

    def __iadd__(self, other: "IndexProcessResult") -> "IndexProcessResult":
        self.finished.extend(other.finished)
        self.failed.extend(other.failed)
        return self


class IndexProcess(Process):

    def __init__(self, 
                 task_queue: Queue, 
                 progress_queue: Queue, 
                 results_queue: Queue,
                 options: IndexOptions, 
                 db_factory: Callable[[], AppDatabase]
                 ):
        super().__init__()
        self.task_queue = task_queue
        self.progress_queue = progress_queue
        self.results_queue = results_queue
        self.db_factory = db_factory
        
        self.options = options
        self.finished_songs: list[str] = list()
        self.failed_songs: list[str] = list()

    
    def run(self):
        
        db = self.db_factory()

        while True:

            try:
                file_path = self.task_queue.get(block=False)
            except queue.Empty: # we finished all tasks
                result = IndexProcessResult(self.finished_songs, self.failed_songs)
                self.results_queue.put(result)
                db.close()
                break
            
            try:
                res = self._index_file(file_path, db)
            except Exception as e:
                self.failed_songs.append(file_path)
                continue
            else:
                if not res:
                    self.failed_songs.append(file_path)
                else:
                    self.finished_songs.append(file_path)
            finally:
                self.progress_queue.put(1) # signal that a song is finished
            

    def _index_file(self, file_path: str, db: AppDatabase):

        if self._should_be_discarded(file_path):
            return False
        
        preprocessed_audio = preprocess_audio_file(file_path)
        fingerprints = self._get_fingerprints(preprocessed_audio)
        tags = self._get_tags(file_path)

        song = Song(
            id=None,
            title=tags.title,
            artist_name=tags.artist,
            album_name=tags.album,
            file_path=file_path,
            duration_sec=preprocessed_audio.duration_seconds,
            sample_rate=preprocessed_audio.rate
        )

        song_id = db.insert_song(song)
        db.insert_fingerprints(song_id=song_id, fingerprints=fingerprints)

        return True


    def _get_fingerprints(self, preprocessed_audio: PreprocessedAudio):
        fingerprints = generate_fingerprints(preprocessed_audio, window_size=WINDOW_SIZE, hop_size=HOP_SIZE)
        return fingerprints

    def _get_tags(self, file_path: str) -> Tags:
        tags = TinyTag.get(file_path)
        title = tags.title or os.path.splitext(os.path.basename(file_path))[0]
        artist = tags.artist
        album = tags.album

        return Tags(title, artist, album)


    def _should_be_discarded(self, file_path: str):

        duration = audiofile.duration(file_path, sloppy=True)
        options = self.options

        return options.max_duration_sec and duration > options.max_duration_sec
