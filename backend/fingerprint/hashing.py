import hashlib


def hash_fingerprints(fingerprints):

    hashes = []
    for i in range(len(fingerprints)):
        f1, f2, delta_frame = fingerprints[i][0]
        h = _simple_hash(int(f1), int(f2), int(delta_frame))
        hashes.append((h, int(fingerprints[i][1])))

    return hashes

# 31-bit hash
def _simple_hash(f1, f2, delta_t):
    return ((f1 & 0x3FF) << 21) | ((f2 & 0x3FF) << 11) | (delta_t) & 0x7FF