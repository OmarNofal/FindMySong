from dataclasses import dataclass


@dataclass
class Song:
    id: int
    title: str
    artist_name: str
    album_name: str
    file_path: str
    duration_sec: int
    sample_rate: int