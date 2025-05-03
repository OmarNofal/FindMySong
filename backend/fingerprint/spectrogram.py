import numpy as np
import matplotlib.pyplot as plt 

def _generate_spectrogram(windows: list[np.ndarray]):

    # num_windows = len(windows)
    # window_size = len(windows[0])
    # spectrogram = np.zeros((window_size // 2 + 1, num_windows))


    # for idx, w in enumerate(windows):
    #     spectrum = np.abs(np.fft.rfft(w))
    #     spectrogram[:, idx] = spectrum

    spectrogram = np.abs(np.fft.rfft(windows, axis=1))**2

    return spectrogram.T

def _plot_and_save_spectrogram(file_name: str, spectrogram: np.ndarray, window_size, hop_size, rate):
    ime_axis = np.arange(spectrogram.shape[1]) * hop_size / rate
    freq_axis = np.fft.rfftfreq(window_size, d=1.0/rate)

    plt.figure(figsize=(10, 6))
    plt.imshow(10 * np.log10(spectrogram + 1e-9),
                aspect='auto',
                origin='lower',
                cmap='inferno')
    
    plt.xlabel('Time Frame')
    plt.ylabel('Frequency (Hz)')
    plt.title('Spectrogram')
    plt.colorbar(label='Amplitude (dB)')
    plt.tight_layout()
    plt.savefig(file_name)
