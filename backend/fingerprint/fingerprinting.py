import numpy as np
import scipy.ndimage
from config.constants import FANOUT, HOP_SIZE, NEIGHBORHOOD_SIZE, WINDOW_SIZE
from preprocessing.audio_preprocessing import PreprocessedAudio, preprocess_audio_file
import matplotlib.pyplot as plt
from .spectrogram import _generate_spectrogram, _plot_and_save_spectrogram 
import scipy
from fingerprint.hashing import hash_fingerprints




def generate_fingerprints(audio: PreprocessedAudio, window_size: int = WINDOW_SIZE, hop_size: int = HOP_SIZE):
    
    windows = _split_into_windows(audio, window_size, hop_size, apply_hanning=True)
    
    spectrogram = _generate_spectrogram(windows)
    spectrogram = 10 * np.log10(spectrogram + 1e-10)
    
    peaks = _generate_peaks(spectrogram)
    fingerprints = _generate_peaks_pairs(peaks, window_size, hop_size, audio.rate, fanout=FANOUT)
    
    return hash_fingerprints(fingerprints)


_hanning_cache = dict()

def _split_into_windows(audio: PreprocessedAudio, window_size: int, hop_size: int, apply_hanning: bool = True):
    
    signal = audio.signal

    if not window_size in _hanning_cache:
        hanning_fn = np.hanning(window_size) if apply_hanning else 1
        _hanning_cache[window_size] = hanning_fn
    else:
        hanning_fn = _hanning_cache[window_size]

    num_windows = 1 + (len(signal) - window_size) // hop_size
    shape = (num_windows, window_size)
    strides = (hop_size * signal.strides[0], signal.strides[0])
    windows = np.lib.stride_tricks.as_strided(signal, shape=shape, strides=strides)
    
    return windows * hanning_fn 



def _generate_peaks(spectrogram: np.ndarray, neighborhood_size: int = NEIGHBORHOOD_SIZE, max_peaks_per_frame: int = 8):
    
    filter_size = neighborhood_size
    sensitivity = 2

    local_mean = scipy.ndimage.uniform_filter(spectrogram, size=filter_size)
    threshold_mask = spectrogram > (local_mean * sensitivity)
    local_max = scipy.ndimage.grey_dilation(spectrogram, filter_size) == spectrogram
    peaks_mask = threshold_mask & local_max

    peak_coords = np.argwhere(peaks_mask)  # shape: (N, 2), each row is [freq, time]
    amplitudes = spectrogram[peaks_mask]
    time_indices = peak_coords[:, 1]
    freq_indices = peak_coords[:, 0]


    peaks_by_frame = {}
    for t in np.unique(time_indices):
        idxs = np.where(time_indices == t)[0]
        frame_peaks = list(zip([t]*len(idxs), freq_indices[idxs], amplitudes[idxs]))
        frame_peaks.sort(key=lambda x: x[2], reverse=True)
        for tp in frame_peaks[:max_peaks_per_frame]:
            peaks_by_frame.setdefault(t, []).append((tp[0], tp[1]))

    result = [p for plist in peaks_by_frame.values() for p in plist]
    result.sort()

    return result




def _generate_peaks_pairs(peaks, window_size, hop_size, rate, fanout = FANOUT):
    """
    The peaks must be sorted
    """
    if len(peaks) == 0:
        return list()
    
    time_idx, freq_idx = zip(*peaks)
    
    min_time_delta_ms = 0
    max_time_delta_ms = 1500

    min_frame_delta = (min_time_delta_ms * rate) / ( hop_size * 1000 )
    max_frame_delta = (max_time_delta_ms * rate) / ( hop_size * 1000 ) 

    fingerprints = list()

    for i, p in enumerate(peaks):

        a_t_frame, a_freq = p
        a_t_msec = int(((a_t_frame * hop_size) / rate) * 1000)

        for j in range(1, fanout + 1):
            if i + j >= len(peaks):
                break

            b_t_frame, b_freq = peaks[i + j]
            delta_t_frame = b_t_frame - a_t_frame

            if delta_t_frame < min_frame_delta or delta_t_frame > max_frame_delta:
                continue

            else:
                fingerprint = ((a_freq, b_freq, delta_t_frame), a_t_msec)
                fingerprints.append(fingerprint)

    return fingerprints
