from dataclasses import dataclass
from typing import Union
from abc import ABC, abstractmethod



@dataclass
class SongIndexSuccess:
    file_path: str
    song_name: str
    artist: str
    index_duration_msec: int
    db_id: int
    is_skipped: bool


class Reason(ABC):
    
    @abstractmethod
    def human_readable(self) -> str:
        pass

@dataclass
class ReasonTooLong(Reason):
    duration_sec: int

    def human_readable(self):
        return f"Too long ({int(self.duration_sec)}s)"
    
@dataclass
class ReasonUnknown(Reason):
    exception: Exception

    def human_readable(self):
        return f"Unknown: {self.exception}"

@dataclass
class ReasonBadFile(Reason):
    
    def human_readable(self):
        return "Bad file format"


@dataclass
class SongIndexError:
    file_path: str
    song_name: str
    artist: str 
    reason: Reason
