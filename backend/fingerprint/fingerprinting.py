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
    ##_plot_and_save_spectrogram('my_spec.png', spectrogram, window_size, hop_size, audio.rate)
    peaks = _generate_peaks(spectrogram)
    fingerprints = _generate_peaks_pairs(peaks, window_size, hop_size, audio.rate, fanout=FANOUT)
    return hash_fingerprints(fingerprints)


def _split_into_windows(audio: PreprocessedAudio, window_size: int, hop_size: int, apply_hanning: bool = True):
    
    signal = audio.signal
    hanning_fn = np.hanning(window_size) if apply_hanning else 1

    num_windows = 1 + (len(signal) - window_size) // hop_size
    shape = (num_windows, window_size)
    strides = (hop_size * signal.strides[0], signal.strides[0])
    windows = np.lib.stride_tricks.as_strided(signal, shape=shape, strides=strides)
    
    return windows * hanning_fn  # apply windowing in a single line

def _generate_peaks(spectrogram: np.ndarray, neighborhood_size: int = NEIGHBORHOOD_SIZE):
    
    filter_size = neighborhood_size
    sensitivity = 2
    
    # Step 1: Local mean filter for adaptive thresholding
    local_mean = scipy.ndimage.uniform_filter(spectrogram, size=filter_size)
    threshold_mask = spectrogram > (local_mean * sensitivity)

    # Step 2: Find local maxima using grey dilation
    local_max = scipy.ndimage.grey_dilation(spectrogram, filter_size) == spectrogram

    # Step 3: Combine both to get strong, meaningful peaks
    peaks = threshold_mask & local_max

    # Step 4: Get peak coordinates
    freq_idx, time_idx = np.where(peaks)
    peak_tuples = list(zip(time_idx, freq_idx))

    # Step 5: Limit peaks per frame
    from collections import defaultdict
    frame_peaks = defaultdict(list)

    for t, f in peak_tuples:
        frame_peaks[t].append((t, f, spectrogram[f, t]))  # add amplitude

    max_peaks_per_frame = 10
    # Only keep top-N peaks by amplitude per frame
    pruned_peaks = []
    for peaks_in_frame in frame_peaks.values():
        peaks_in_frame.sort(key=lambda x: x[2], reverse=True)  # sort by amplitude
        pruned = [(t, f) for t, f, _ in peaks_in_frame[:max_peaks_per_frame]]
        pruned_peaks.extend(pruned)

    pruned_peaks.sort()
    return pruned_peaks

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

    max_freq_delta = 300 # experiment with this number

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
            # elif abs(b_freq - a_freq) > max_freq_delta:
            #     continue
            else:
                fingerprint = ((a_freq, b_freq, delta_t_frame), a_t_msec)
                fingerprints.append(fingerprint)

    return fingerprints
