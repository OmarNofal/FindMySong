
from dataclasses import dataclass, field
from multiprocessing import Process, Queue
import os
import queue
from time import time, time_ns
from typing import Callable, List

import audiofile
from tinytag import TinyTag

from config.constants import HOP_SIZE, WINDOW_SIZE
from database.db import AppDatabase
from fingerprint.fingerprinting import generate_fingerprints
from indexing.index_result import Reason, ReasonBadFile, ReasonTooLong, ReasonUnknown, SongIndexError, SongIndexSuccess
from model.song import Song
from preprocessing.audio_preprocessing import PreprocessedAudio, preprocess_audio_file



@dataclass
class IndexProcessOptions:
    max_duration_sec: int

@dataclass
class Tags:
    title: str
    artist: str
    album: str


class IndexProcess(Process):

    def __init__(self, 
                 task_queue: Queue, 
                 progress_queue: Queue,
                 options: IndexProcessOptions, 
                 db_factory: Callable[[], AppDatabase]
                 ):
        super().__init__()
        self.task_queue = task_queue
        self.progress_queue = progress_queue
        self.db_factory = db_factory
        
        self.options = options

    
    def run(self):
        
        db = self.db_factory()

        while True:

            try:
                file_path = self.task_queue.get(block=False)
            except queue.Empty: # we finished all tasks
                db.close()
                break
            
            try:
                res = self._index_file(file_path, db)
            except Exception as e:
                res = SongIndexError(
                    file_path=file_path,
                    song_name=os.path.basename(file_path),
                    artist="",
                    reason=ReasonUnknown(e)
                )
            finally:
                self.progress_queue.put(res) # signal that a song is finished
            

    def _index_file(self, file_path: str, db: AppDatabase) -> SongIndexSuccess | SongIndexError:
        
        start_time = time_ns()

        tags = self._get_tags(file_path)

        reason_to_discard = self._reason_to_discard(file_path)
        if reason_to_discard is not None:
            return SongIndexError(
                file_path=file_path,
                song_name=tags.title,
                artist=tags.artist,
                reason=reason_to_discard
            )
        
        song_db_id = db.get_song_id(tags.title, tags.artist, tags.album)
        if song_db_id is not None:
            
            end_time = time_ns()
            total_time_ms = (end_time - start_time) / 1_000_000 

            return SongIndexSuccess(
            file_path=file_path,
            song_name = tags.title,
            artist=tags.artist,
            index_duration_msec=total_time_ms,
            db_id=song_db_id,
            is_skipped=True
        )

        try:
            preprocessed_audio = preprocess_audio_file(file_path)
            fingerprints = self._get_fingerprints(preprocessed_audio)
        except Exception as e:
            return SongIndexError(
                file_path=file_path,
                song_name=tags.title,
                artist=tags.artist,
                reason=ReasonBadFile()
            )
            

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

        end_time = time_ns()
        total_time_ms = (end_time - start_time) / 1_000_000 

        return SongIndexSuccess(
            file_path=file_path,
            song_name = song.title,
            artist=song.artist_name,
            index_duration_msec=total_time_ms,
            db_id=song_id,
            is_skipped=False
        )


    def _get_fingerprints(self, preprocessed_audio: PreprocessedAudio):
        fingerprints = generate_fingerprints(preprocessed_audio, window_size=WINDOW_SIZE, hop_size=HOP_SIZE)
        return fingerprints

    def _get_tags(self, file_path: str) -> Tags:
        tags = TinyTag.get(file_path, ignore_errors=True)
        title = tags.title or os.path.splitext(os.path.basename(file_path))[0]
        artist = tags.artist
        album = tags.album

        return Tags(title, artist, album)


    def _reason_to_discard(self, file_path: str) -> Reason:

        duration = audiofile.duration(file_path, sloppy=True)
        options = self.options

        if options.max_duration_sec and duration > options.max_duration_sec:
            return ReasonTooLong(duration)

        return None 
