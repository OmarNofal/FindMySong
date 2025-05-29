


from dataclasses import dataclass


@dataclass
class IndexConfig:
    num_workers: int = 1
    max_duration_sec: int = None
    print_tables: bool = False