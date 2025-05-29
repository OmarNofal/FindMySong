import io
import psycopg2
from psycopg2.extras import execute_batch
from typing import List, Tuple
from itertools import batched
from model.song import Song

class AppDatabase:
    def __init__(self, dbname, user, password, host='localhost', port=5432):
        self.conn = psycopg2.connect(
            dbname=dbname,
            user=user,
            password=password,
            host=host,
            port=port
        )
        self.conn.autocommit = True


    def create_tables(self):
        return self._create_tables()

    def _create_tables(self):
        with self.conn.cursor() as cur:
            cur.execute("""
                CREATE TABLE IF NOT EXISTS songs (
                    id SERIAL PRIMARY KEY,
                    title TEXT NOT NULL,
                    artist_name TEXT,
                    album_name TEXT,
                    file_path TEXT,
                    duration_sec INT,
                    sample_rate INT
                );
            """)
            cur.execute("""
                CREATE TABLE IF NOT EXISTS fingerprints (
                    hash INT NOT NULL,
                    time_offset_msec INT NOT NULL,
                    song_id INTEGER REFERENCES songs(id)
                );
            """)
            cur.execute("""
                CREATE INDEX IF NOT EXISTS idx_fingerprint_hash ON fingerprints(hash);
            """)

    def insert_song(self, song: Song) -> int:
        with self.conn.cursor() as cur:
            cur.execute("""
                INSERT INTO songs (title, artist_name, album_name, duration_sec, file_path, sample_rate)
                VALUES (%s, %s, %s, %s, %s, %s) RETURNING id;
            """, (song.title, song.artist_name, song.album_name, song.duration_sec, song.file_path, song.sample_rate))
            return cur.fetchone()[0]

    def insert_fingerprints(self, song_id: int, fingerprints: List[Tuple[int, int]]):

        with self.conn.cursor() as cur:
            buffer = io.StringIO()
            for hash_val, time_offset in fingerprints:
                buffer.write(f"{hash_val}\t{time_offset}\t{song_id}\n")
            buffer.seek(0)
            cur.copy_from(buffer, 'fingerprints', columns=('hash', 'time_offset_msec', 'song_id'))

    def find_matches(self, hashes: List[int]) -> List[Tuple[int, float, int]]:
        with self.conn.cursor() as cur:
            query = """
                SELECT hash, time_offset_msec, song_id
                FROM fingerprints
                WHERE hash = ANY(%s);
            """
            cur.execute(query, (hashes,))
            return cur.fetchall()
        
    def get_song(self, song_id: int) -> Song | None:
        with self.conn.cursor() as cur:
            query = """
                SELECT * FROM songs
                WHERE id = %s
            """
            
            cur.execute(query, (song_id,))
            result = cur.fetchone()
            
            if result is None:
                return None
            
            song = Song(
                result[0],
                result[1],
                result[2],
                result[3],
                result[4],
                result[5],
                result[6]
            )
            return song
        
    def get_number_of_songs(self) -> int:
        with self.conn.cursor() as cur:
            query = """
                SELECT COUNT(*)
                FROM songs;
            """
            cur.execute(query)
            return cur.fetchone()[0]

    def close(self):
        self.conn.close()
