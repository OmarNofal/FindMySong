import audiofile
import matplotlib.pyplot as plt
import numpy as np
import scipy.ndimage
import scipy.signal


### Loading the audio file ###
### ---------------------- ###

audio_path = 'audio_files/love.mp3'

signal, rate = audiofile.read(audio_path, always_2d=False)

if signal.ndim > 1:
    signal = signal.mean(axis=0)

signal = signal / np.max(np.abs(signal))

print("Num of samples", signal.shape)
print("Sampling rate:", rate)

duration_seconds = signal.shape[0] / rate 
time = np.linspace(0, duration_seconds, signal.shape[0])

plt.figure(figsize=(12, 4))
plt.xlabel("Time [sec]")
plt.ylabel("Amplitude")
plt.title("Summerbummer audio signal")
plt.plot(time[11000000:], signal[11000000:])
plt.savefig("out/waveform.png")


### Windowing and applying FFT ###
### -------------------------- ###
window_size = 2048 # Each window contains 2048 samples
hop_size = 512 # The distance between each window is 512 samples (Meaning, there will be overlap between windows)
# overlap = window_size - hop_size
 
hanning_fn = np.hanning(window_size)

num_windows = (len(signal) - window_size) // hop_size
spectrogram = np.zeros((window_size // 2 + 1, num_windows))

for i in range(num_windows):

    start = i * hop_size
    end = start + window_size

    window = signal[start:end] * hanning_fn
    spectrum = np.fft.rfft(window)
    spectrogram[:, i] = np.abs(spectrum)

# Plotting the spectogram
time_axis = np.arange(spectrogram.shape[1]) * hop_size / rate
freq_axis = np.fft.rfftfreq(window_size, d=1.0/rate)

plt.figure(figsize=(10, 6))
plt.imshow(10 * np.log10(spectrogram + 1e-9),
            aspect='auto',
            origin='lower',
            extent=[0, len(signal)/rate, 0, rate/2],
            cmap='viridis')
plt.xlabel('Time (s)')
plt.ylabel('Frequency (Hz)')
plt.title('Spectrogram')
plt.colorbar(label='Amplitude (dB)')
plt.tight_layout()
plt.savefig('out/spectrogram.png')
print("Min mag:", np.min(spectrogram))
print("Max mag:", np.max(spectrogram))

# # Compute spectrogram
# f, t, Sxx = scipy.signal.spectrogram(
#     signal,
#     fs=rate,
#     window='hann',
#     nperseg=2048,
#     noverlap=1536,
#     detrend=False,
#     scaling='spectrum',
#     mode='magnitude'  # 'psd' for power spectral density
# )
# Sxx = 10 * np.log10(Sxx + 1e-9)
# # Plot (linear scale)
# plt.figure(figsize=(10, 5))
# plt.imshow(Sxx,
#             aspect='auto',
#             origin='lower',
#             extent=[0, len(signal)/rate, 0, rate/2],
#             cmap='viridis')
# plt.ylabel('Frequency [Hz]')
# plt.xlabel('Time [sec]')
# plt.title('Spectrogram (Linear Magnitude)')
# plt.colorbar(label='Magnitude')
# plt.tight_layout()
# plt.savefig("out/scipy_spec.png")

filter_size = (20, 20)
local_max = scipy.ndimage.maximum_filter(spectrogram, filter_size) == spectrogram
threshold = np.mean(spectrogram) * 2

peaks = (spectrogram > threshold) & local_max


freq_idx, time_idx = np.where(peaks)

peaks = list(zip(time_idx, freq_idx))
peaks.sort()
print(peaks)

times, freqs = zip(*peaks)

plt.figure(figsize=(10, 6))
# plt.imshow(peaks,
#             aspect='auto',
#             origin='lower',
#             extent=[0, len(signal)/rate, 0, np.max(freqs)],
#             cmap='gray')
plt.xlabel('Time (s)')
plt.ylabel('Frequency (Hz)')
plt.title('Spectrogram Peaks')
plt.scatter(times, freqs, color='red', s=3, label="Peaks")

plt.savefig('out/peaks.png')


print(f"There are {len(peaks)} peaks")

### Creating fingerprints ###
### --------------------- ###
fanout = 4
min_delta = 0.01 * rate / hop_size
max_delta = 5 * rate / hop_size
max_freq_delta = 150

fingerprints = []

for i in range(len(peaks)):

    a_time, a_freq = peaks[i] 

    for j in range(1, fanout + 1):
        if i + j >= len(peaks):
            break
        
        t_time, t_freq = peaks[i + j]
        delta_t = t_time - a_time # in frames

        if delta_t < min_delta or delta_t > max_delta:
            continue
        if abs(a_freq - t_freq) > max_freq_delta:
            continue

        fingerprints.append(((a_freq, t_freq, delta_t), a_time))

plt.figure(figsize=(14, 6))

# Plot just the peaks for context
times = [t for t, f in peaks]
freqs = [f for t, f in peaks]
plt.scatter(times, freqs, s=2, color='gray', alpha=0.3)

# Plot arrows between fingerprint pairs
for (a_freq, b_freq, delta_t), a_time in fingerprints[0:1000]:
    b_time = a_time + delta_t
    plt.arrow(a_time, a_freq, delta_t, b_freq - a_freq,
              head_width=5, head_length=0.05, color='blue', alpha=0.5, length_includes_head=True)

plt.xlabel('Time (s)')
plt.ylabel('Frequency (Hz)')
plt.title('Fingerprint Connections')
plt.tight_layout()
plt.savefig("out/fingerprints_visualization.png")