import matplotlib.pyplot as plt
import numpy as np

from config.constants import DEFAULT_SAMPLE_RATE, FANOUT, HOP_SIZE, WINDOW_SIZE
from fingerprint.fingerprinting import _generate_peaks, _generate_peaks_pairs, _split_into_windows
from fingerprint.spectrogram import _generate_spectrogram, _plot_and_save_spectrogram
from preprocessing.audio_preprocessing import preprocess_audio_file

song_path = './audio_files/love.mp3'


audio = preprocess_audio_file(song_path)
print("Audio rate: ", audio.rate)
print("Audio shape: ", audio.signal.shape)
x = np.linspace(0, audio.duration_seconds, audio.signal.shape[0])


plt.figure(figsize=(14, 4))
plt.plot(x, audio.signal)
plt.title('Song Waveform')
plt.xlabel('Time (s)')
plt.ylabel('Amplitude')
plt.savefig('./out/waveform.png')


windows = _split_into_windows(audio, WINDOW_SIZE, HOP_SIZE)
print(windows.shape)
assert windows.shape == (int((audio.signal.shape[0] - WINDOW_SIZE) / HOP_SIZE) + 1, WINDOW_SIZE)

spectrogram = _generate_spectrogram(windows)
spectrogram = 10 * np.log10(spectrogram + 1e-10)  # avoid log(0)
print(spectrogram.shape)
_plot_and_save_spectrogram('./out/spectrogram_love.png', spectrogram, WINDOW_SIZE, HOP_SIZE, DEFAULT_SAMPLE_RATE)

# peak detection
peaks = _generate_peaks(spectrogram)
print("Number of peaks: ", len(peaks))
plt.figure(figsize=(14,4))
plt.imshow(spectrogram, origin='lower', aspect='auto', cmap='inferno')
peaks_time, peaks_freq = zip(*peaks)
plt.scatter(peaks_time, peaks_freq, color='blue', marker='^', label='Peaks')
plt.legend()
plt.savefig('./out/peaks.png')

# peak pairing
fingerprints = _generate_peaks_pairs(peaks, WINDOW_SIZE, HOP_SIZE, DEFAULT_SAMPLE_RATE, FANOUT)
plt.imshow(spectrogram, aspect='auto', origin='lower', cmap='inferno')

# For each fingerprint (which consists of two peaks), plot a line
for fingerprint in fingerprints:
    (a_freq, b_freq, delta_frame), a_t_msec = fingerprint
    plt.plot([(a_t_msec * DEFAULT_SAMPLE_RATE) / ( HOP_SIZE * 1000 ) , (a_t_msec * DEFAULT_SAMPLE_RATE) / ( HOP_SIZE * 1000 )  + delta_frame], [a_freq, b_freq], color='white', lw=0.5)

plt.xlabel('Time (ms)')
plt.ylabel('Frequency (Hz)')
plt.title('Paired Peaks on Spectrogram')
plt.colorbar(label='Amplitude (dB)')
plt.tight_layout()
plt.savefig('./out/fingerprints.png')