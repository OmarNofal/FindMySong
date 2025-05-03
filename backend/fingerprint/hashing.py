import hashlib


def hash_fingerprints(fingerprints):

    hashes = []
    for i in range(len(fingerprints)):
        f1, f2, delta_t = fingerprints[i][0]
        h = _simple_hash(int(f1), int(f2), int(delta_t))
        hashes.append((h, int(fingerprints[i][1])))

    return hashes

def _simple_hash(f1, f2, delta_t):
    return ((f1 & 0x3FF) << 22) | ((f2 & 0x3FF) << 12) | (delta_t) & 0xFFF