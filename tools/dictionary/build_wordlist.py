#!/usr/bin/env python3
"""Builds app/src/main/assets/dictionaries/en_US.tsv from SCOWL and wordfreq.

SCOWL decides which words are valid, wordfreq decides how common they are. See README.md for details.
"""

import argparse
import hashlib
import tarfile
import unicodedata
import urllib.request
from dataclasses import dataclass, field
from importlib.metadata import version
from pathlib import Path

from wordfreq import zipf_frequency

SCOWL_VERSION = "2020.12.07"
SCOWL_URL = (
    f"https://downloads.sourceforge.net/project/wordlist/SCOWL/{SCOWL_VERSION}/scowl-{SCOWL_VERSION}.tar.gz"
)
SCOWL_SHA256 = "5587667caa20c4891390c2d42dbb4d5c4c3f41bee77af1457ece3ba23fb859cc"

# SCOWL category -> largest size included. Sizes above BASE_MAX_SIZE are rarer words and need RARE_MIN_ZIPF.
SCOWL_LISTS = {
    "english-words": 60,
    "american-words": 60,
    "english-upper": 50,
    "american-upper": 50,
    "english-contractions": 60,
}
BASE_MAX_SIZE = 50
RARE_MIN_ZIPF = 1.5
SHORT_WORD_MAX_SIZE = 35
SINGLE_LETTER_WORDS = {"a", "I"}
UNKNOWN_ZIPF = 1.0
FREQ_SCALE = 30
ALPHABET = set("abcdefghijklmnopqrstuvwxyz'-")

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
OUTPUT = ROOT / "app/src/main/assets/dictionaries/en_US.tsv"


@dataclass
class Word:
    surface: str
    scowl_size: int | None = None
    extra: bool = False
    zipf_override: float | None = None
    flags: set = field(default_factory=set)


def download_scowl(cache: Path) -> Path:
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / f"scowl-{SCOWL_VERSION}.tar.gz"
    if not path.exists():
        print(f"Downloading {SCOWL_URL}")
        urllib.request.urlretrieve(SCOWL_URL, path)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != SCOWL_SHA256:
        raise SystemExit(f"SCOWL checksum mismatch: {digest}")
    return path


def read_scowl(tarball: Path) -> dict[str, int]:
    """Returns every word of the selected SCOWL lists with the smallest size it appears in."""
    words: dict[str, int] = {}
    with tarfile.open(tarball) as tar:
        for member in tar.getmembers():
            name = Path(member.name).name
            if not member.isfile() or "/final/" not in member.name or "." not in name:
                continue
            category, size = name.rsplit(".", 1)
            size = int(size)
            if size > SCOWL_LISTS.get(category, -1):
                continue
            text = tar.extractfile(member).read().decode("iso-8859-1")
            for word in text.split():
                if size > BASE_MAX_SIZE and zipf_frequency(word, "en") < RARE_MIN_ZIPF:
                    continue
                words[word] = min(size, words.get(word, size))
    return words


def read_tsv(path: Path) -> list[list[str]]:
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.split("#", 1)[0].rstrip()
        if line:
            rows.append(line.split("\t"))
    return rows


def fold(word: str) -> str | None:
    """Lowercases a word and strips its accents, or returns None if it doesn't fit the alphabet."""
    word = word.replace("’", "'")
    decomposed = unicodedata.normalize("NFD", word)
    key = "".join(c for c in decomposed if not unicodedata.combining(c)).lower()
    return key if key and set(key) <= ALPHABET else None


def is_possessive(word: str, words: dict[str, Word]) -> bool:
    if word.endswith("'s"):
        return word[:-2] in words
    return word.endswith("s'") and word[:-1] in words


def case_rank(surface: str, key: str) -> int:
    """Prefers lowercase, then Title case, then ALLCAPS, then any other spelling."""
    if surface == key:
        return 0
    if surface == key[:1].upper() + key[1:]:
        return 1
    if surface == key.upper():
        return 2
    return 3


def is_plain(surface: str) -> bool:
    return surface.isascii()


def collect_words(scowl: dict[str, int], extras: list[list[str]], deny: list[list[str]]) -> dict[str, Word]:
    words = {surface: Word(surface, scowl_size=size) for surface, size in scowl.items()}
    for row in extras:
        word = words.setdefault(row[0], Word(row[0]))
        word.extra = True
        if len(row) > 1 and row[1]:
            word.zipf_override = float(row[1])

    extra_words = {row[0] for row in extras}
    for surface in list(words):
        if surface not in extra_words and is_possessive(surface, words):
            del words[surface]

    denied = {row[0]: set(row[1]) if len(row) > 1 else {"x"} for row in deny}
    for surface in list(words):
        flags = denied.get(fold(surface) or surface)
        if flags is None:
            continue
        if "x" in flags:
            del words[surface]
        else:
            words[surface].flags |= flags

    for surface in list(words):
        word = words[surface]
        key = fold(surface)
        short_ok = word.extra or (word.scowl_size is not None and word.scowl_size <= SHORT_WORD_MAX_SIZE)
        if key is not None and len(key) == 1:
            short_ok = word.extra or surface in SINGLE_LETTER_WORDS
        if key is None or (len(key) <= 2 and not short_ok):
            del words[surface]
    return words


def zipf_of(word: Word) -> float:
    if word.zipf_override is not None:
        return word.zipf_override
    zipf = zipf_frequency(word.surface, "en")
    return zipf if zipf > 0 else UNKNOWN_ZIPF


def to_freq(zipf: float) -> int:
    return max(1, min(255, round(zipf * FREQ_SCALE)))


def build_entries(words: dict[str, Word]) -> list[tuple[str, int, str, str | None]]:
    by_key: dict[str, list[Word]] = {}
    for word in words.values():
        by_key.setdefault(fold(word.surface), []).append(word)

    entries = []
    for key, group in by_key.items():
        plain = sorted((w for w in group if is_plain(w.surface)), key=lambda w: case_rank(w.surface, key))
        accented = sorted((w for w in group if not is_plain(w.surface)), key=lambda w: w.surface)
        # One plain spelling per key: "will" beats "Will", "Monday" beats "MONDAY". Lowercase accented spellings
        # (café, résumé) are kept next to it as variants.
        chosen = plain[:1] + [w for w in accented if w.surface == w.surface.lower()]
        if not chosen:
            chosen = accented[:1]
        for word in chosen:
            flags = "".join(sorted(f for f in word.flags if f in "on")) or "-"
            surface = None if word.surface == key else word.surface
            entries.append((key, to_freq(zipf_of(word)), flags, surface))

    entries.sort(key=lambda e: (e[0], -e[1], e[3] or ""))
    return entries


def write_tsv(entries, path: Path):
    header = [
        f"# en_US word list: {len(entries)} entries, generated by tools/dictionary/build_wordlist.py.",
        f"# Words: SCOWL {SCOWL_VERSION} (http://wordlist.aspell.net/), (c) Kevin Atkinson and others,",
        "#   MIT-like license, see LICENSE-en_US.txt.",
        f"# Frequencies: wordfreq {version('wordfreq')} (https://github.com/rspeer/wordfreq), (c) Robyn Speer,",
        "#   data licensed under CC BY-SA 4.0, see LICENSE-en_US.txt.",
        "# Format: key<TAB>freq<TAB>flags[<TAB>surface]; freq = round(zipf * 30), flags: o = offensive,",
        "#   n = never autocorrect to, - = none.",
    ]
    lines = header + ["\t".join(str(f) for f in entry if f is not None) for entry in entries]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", type=Path, default=HERE / ".cache", help="where to keep the SCOWL download")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    args = parser.parse_args()

    scowl = read_scowl(download_scowl(args.cache))
    words = collect_words(scowl, read_tsv(HERE / "extras_en.tsv"), read_tsv(HERE / "deny_en.tsv"))
    entries = build_entries(words)
    write_tsv(entries, args.output)
    print(f"Wrote {len(entries)} entries to {args.output}")


if __name__ == "__main__":
    main()
