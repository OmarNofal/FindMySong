import pprint
import numpy as np
from config.constants import HOP_SIZE, WINDOW_SIZE
from database.db import AppDatabase
from fingerprint.fingerprinting import generate_fingerprints
from preprocessing.audio_preprocessing import preprocess_audio_file, PreprocessedAudio


from collections import defaultdict, Counter


def find_matches_of_file(db: AppDatabase, audio_file_path: str, top_n: int = 5):
    preprocessed_audio = preprocess_audio_file(audio_file_path)
    return get_audio_matches(db, preprocess_audio_file)


def get_audio_matches(db: AppDatabase, audio: PreprocessedAudio, top_n: int = 5):

    fingerprints = generate_fingerprints(audio, WINDOW_SIZE, HOP_SIZE)
    hash_time_pairs = [(f[0], f[1]) for f in fingerprints]  # (hash, time_offset)

    # Find all matches in the database for the query hashes
    matches = db.find_matches([h for h, _ in hash_time_pairs])  # returns (hash, db_time, song_id)

    offset_votes = dict()  # song_id -> Counter of delta_t

    # Build a map from hash to query time
    query_hash_time_map = dict(hash_time_pairs)

    BIN_SIZE = 3 # milliseconds

    for h, db_time, song_id in matches:
        query_time = query_hash_time_map.get(h)
        if query_time is None:
            continue

        delta_t = db_time - query_time
        binned_delta = (delta_t // BIN_SIZE) * BIN_SIZE

        if song_id not in offset_votes:
            offset_votes[song_id] = Counter()
        offset_votes[song_id][binned_delta] += 1


    # Score by the most common delta_t (i.e., peak of the voting histogram)
    scores = {
        song_id: max(votes.values())
        for song_id, votes in offset_votes.items()
    }

    # Sort by score descending
    sorted_scores = sorted(scores.items(), key=lambda x: x[1], reverse=True)

    return sorted_scores



if __name__ == '__main__':

    db = AppDatabase('songs', 'postgres', 'admin')
    sample_path = "C:\\Users\\omarw\\Documents\\Programming\\audio-id\\backend\\audio_files\\recording_05.mp3"
    find_matches_of_file(db, sample_path)