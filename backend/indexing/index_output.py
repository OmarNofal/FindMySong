from typing import List

from prettytable import PrettyTable
from termcolor import colored
from indexing.index_result import SongIndexError, SongIndexSuccess


def _print_success_songs(r: List[SongIndexSuccess]):
    
    cols = ['Database Id', 'Title', 'Artist', 'Index Duration (s)', 'Is Skipped?']
    table = PrettyTable(cols)

    if len(r) > 0:
        print(colored("Indexed Songs", 'green', attrs=['bold', 'underline']))
        for s in r:
            table.add_row([s.db_id, s.song_name, s.artist, round(s.index_duration_msec / 1000, 2), "Yes" if s.is_skipped else "No"], divider=True)
    
    print(table)

def _print_failed_songs(r: List[SongIndexError]):

    cols = ['Title', 'Artist', 'Reason']
    table = PrettyTable(cols)

    if len(r) > 0:
        print(colored("Failed Songs", 'red', attrs=['bold', 'underline']))
        for s in r:
            table.add_row([s.song_name, s.artist, s.reason.human_readable()], divider=True)

    print(table)
