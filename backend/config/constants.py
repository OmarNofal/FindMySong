# Default sampling rate (Hz) used to downsample audio before processing.
# Lower sample rates reduce data size and processing time while retaining relevant features.
DEFAULT_SAMPLE_RATE = 11025

# Size of the FFT window in samples.
# Larger window gives better frequency resolution but worse time resolution.
WINDOW_SIZE = 2048

# Number of samples to skip between windows (i.e., stride).
# Smaller hop size means more overlap and higher time resolution.
HOP_SIZE = 512

# Size of the neighborhood (frequency bins × time bins) when searching for peaks.
# Affects how close or far apart spectral peaks can be to form fingerprint pairs.
NEIGHBORHOOD_SIZE = (25, 25)

# Number of target points (anchor pairs) created per peak.
# More fanout means more robust matching but also increases hash count and database size.
FANOUT = 8
