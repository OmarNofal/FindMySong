import audiofile
import numpy as np
import dataclasses
import contextlib
import os
from scipy.signal import resample

from config.constants import DEFAULT_SAMPLE_RATE

@dataclasses.dataclass
class PreprocessedAudio:
    signal: np.ndarray
    rate: int
    duration_seconds: float


def preprocess_audio_file(path: str, target_rate: int = DEFAULT_SAMPLE_RATE, mono: bool = True) -> PreprocessedAudio:
    
    with suppress_output():
        signal, rate = audiofile.read(path)

    if signal.ndim > 1:
        signal = signal.mean(axis=0)

    if rate != target_rate:
        cur_duration = signal.shape[0] / rate
        target_len = int(cur_duration * target_rate)
        signal = resample(signal, target_len)


    signal = signal / np.max(np.abs(signal))

    duration_seconds = signal.shape[0] / target_rate

    return PreprocessedAudio(signal, target_rate, duration_seconds)


@contextlib.contextmanager
def suppress_output():
    with open(os.devnull, 'w') as fnull:
        with contextlib.redirect_stdout(fnull), contextlib.redirect_stderr(fnull):
            yield