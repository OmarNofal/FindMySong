from collections import defaultdict, Counter
from dataclasses import dataclass
import math
from scipy.signal import resample
import numpy as np
import time

from database.db import AppDatabase
from matching.matching import get_audio_matches
from preprocessing.audio_preprocessing import PreprocessedAudio

@dataclass
class SessionConfiguration:
    in_sample_rate: int
    target_sample_rate: int
    dtype: str              # 'float32' or 'int16'
    topn: int
    chunk_time_msec: int
    stride_msec: int        # how often to shift the window (e.g., 500ms)


class SongIdSession:
    """
    Class for real-time song identification from streaming audio
    """

    def __init__(self, db: AppDatabase, config: SessionConfiguration):
        self.db = db
        self.config = config
        self.is_match_found = False
        self.bytes_buffer = bytearray()
        self.results = defaultdict(int)
        self.sample_size = 4 if config.dtype == 'float32' else 2 if config.dtype == 'int16' else None
        if self.sample_size is None:
            raise NotImplementedError("Only float32 and int16 dtypes supported")
        self.last_match_time = 0

    def push_bytes(self, bytes_chunk: bytearray):

        if self.is_match_found:
            return

        self.bytes_buffer.extend(bytes_chunk)

        while self.buffer_has_enough_bytes():
            self.perform_chunk_matching()

    def buffer_has_enough_bytes(self) -> bool:
        required_bytes = math.ceil(self.config.chunk_time_msec / 1000) * self.config.in_sample_rate * self.sample_size
        return len(self.bytes_buffer) >= required_bytes

    def perform_chunk_matching(self):
        chunk_bytes = self.bytes_buffer[:self.required_bytes()]
        self.bytes_buffer = self.bytes_buffer[self.stride_bytes():]  # slide the window

        chunk_data = np.frombuffer(chunk_bytes, dtype=self.config.dtype)
        
        # Downmix to mono if needed
        if chunk_data.ndim == 2:
            chunk_data = np.mean(chunk_data, axis=1)

        duration_sec = len(chunk_data) / self.config.in_sample_rate

        # Resample if needed
        if self.config.in_sample_rate != self.config.target_sample_rate:
            num_samples = int(duration_sec * self.config.target_sample_rate)
            chunk_data = resample(chunk_data, num_samples)

        preprocessed = PreprocessedAudio(chunk_data, self.config.target_sample_rate, duration_sec)

        matches = get_audio_matches(self.db, preprocessed, self.config.topn)


        for song_id, score in matches:
            self.results[song_id] += score

        self.check_if_results_ready()

    def check_if_results_ready(self):
        top_matches = sorted(self.results.items(), key=lambda x: x[1], reverse=True)[:self.config.topn]

        if len(top_matches) < 2:
            return

        top1, top2 = top_matches[0], top_matches[1]
        score_gap = top1[1] - top2[1]

        if top1[1] > 30 or (top1[1] > 20 and score_gap > 10):
            self.is_match_found = True

    def required_bytes(self):
        return math.ceil(self.config.chunk_time_msec / 1000) * self.config.in_sample_rate * self.sample_size

    def stride_bytes(self):
        return math.ceil(self.config.stride_msec / 1000) * self.config.in_sample_rate * self.sample_size
