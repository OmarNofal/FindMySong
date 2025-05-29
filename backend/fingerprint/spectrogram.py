import numpy as np
import matplotlib.pyplot as plt 

import pyfftw
import numpy as np

def _generate_spectrogram(windows: list[np.ndarray]):
    
    # Convert list to numpy array if not already
    windows_np = np.asarray(windows)
    
    fft_result = pyfftw.interfaces.numpy_fft.rfft(windows_np, axis=1)
    
    spectrogram = np.abs(fft_result) ** 2
    
    return spectrogram.T

def _plot_and_save_spectrogram(file_name: str, spectrogram: np.ndarray, window_size, hop_size, rate):
    time_axis = np.arange(spectrogram.shape[1]) * hop_size / rate
    freq_axis = np.fft.rfftfreq(window_size, d=1.0/rate)

    extent = [time_axis[0], time_axis[-1], freq_axis[0], freq_axis[-1]]
    plt.figure(figsize=(10, 6))
    plt.imshow(spectrogram,
                aspect='auto',
                origin='lower',
                extent=extent,
                cmap='inferno')
    
    plt.xlabel('Time Frame')
    plt.ylabel('Frequency (Hz)')
    plt.title('Spectrogram')
    plt.colorbar(label='Amplitude (dB)')
    plt.tight_layout()
    plt.savefig(file_name)
