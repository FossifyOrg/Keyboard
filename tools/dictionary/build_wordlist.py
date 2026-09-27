#!/usr/bin/env python3
"""Builds the word lists behind word suggestions, app/src/main/assets/dictionaries/<locale>.tsv.

English words come from SCOWL, the words of other languages from the Hunspell dictionaries of LibreOffice, and wordfreq
decides how common they are. See README.md for details.
"""

import argparse
import hashlib
import json
import tarfile
import textwrap
import unicodedata
import urllib.request
from concurrent.futures import ProcessPoolExecutor
from dataclasses import dataclass, field
from importlib.metadata import version
from itertools import product
from pathlib import Path

from wordfreq import get_frequency_dict, zipf_frequency

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
HEADER_WIDTH = 110
ALPHABET = set("abcdefghijklmnopqrstuvwxyz'-")

# Folding, like Alphabet.kt: these letters are spelled out, and these chars are apostrophes and hyphens.
EXPANSIONS = {"ß": "ss", "æ": "ae", "œ": "oe"}
APOSTROPHE_LIKE = "’‘ʼ`´"
HYPHEN_LIKE = "‐‑"

# German words with up to this many "ss" are also tried with "ß" instead
MAX_SHARP_S = 3

# The Hunspell dictionaries of LibreOffice, at a pinned commit. Files are checked against their SHA-256.
LIBREOFFICE_COMMIT = "32b006a2c22a4ac7e8ed3f03346f7b3d85a970a4"
LIBREOFFICE_URL = f"https://raw.githubusercontent.com/LibreOffice/dictionaries/{LIBREOFFICE_COMMIT}"

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
ASSETS = ROOT / "app/src/main/assets/dictionaries"


@dataclass(frozen=True)
class Hunspell:
    """A Hunspell dictionary of LibreOffice: its path without extension and the SHA-256 of its .dic and .aff files."""

    path: str
    dic_sha256: str
    aff_sha256: str


@dataclass(frozen=True)
class Language:
    """A language whose valid words come from Hunspell dictionaries. See README.md for what each setting does."""

    locale: str
    wordfreq: str
    hunspell: tuple[Hunspell, ...]
    credit: str
    single_letters: frozenset[str]
    max_entries: int
    min_zipf: float = 1.5
    short_min_zipf: float = 3.0
    joined_min_zipf: float = 3.0
    sharp_s: bool = False
    elisions: frozenset[str] = frozenset()
    apostrophe_words: int = 0
    accentless_max_gap: float | None = None


LANGUAGES: dict[str, Language] = {}

LANGUAGES["es"] = Language(
    locale="es",
    wordfreq="es",
    # Spain first, then the Americas: a word is valid if any of them has it. es_UY is left out, as spylls can't read
    # its affix file; es_AR has the words of the Río de la Plata
    hunspell=(
        Hunspell(
            "es/es_ES",
            "6975dddec3d5d2c676069537bc67b4b5f786c65c5d4cf6703a82acf779ac9ec1",
            "e73a9bf8e1383f4986a5dc9e2fbed49371c0c61f511c626d15586bd433c1cad9",
        ),
        Hunspell(
            "es/es_AR",
            "4fe3e193425bb3841ee218d36a9fc95d85b8daa8909363bdbba55a2f9f4e3d1d",
            "e9a96514295f8664db270d024c345700ccb9410a384eb71aec0fe5f0931d8c70",
        ),
        Hunspell(
            "es/es_BO",
            "c882b993a0ae83c94ab94c4b84a8f8300979e0e44fc4dceb2f15eaba521a30e7",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_CL",
            "e249a50ed9c9b939801bd2ba12099c9decb59217782c59fc65e7f72831b0b401",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_CO",
            "10f5e38e1c6898194cd860c237b1569725f721eb5eb0705e3a7158c69652c226",
            "243d5dc50b68261dd10118799724beb95a6c19d4f9d6848ebfc977430bf2f8e2",
        ),
        Hunspell(
            "es/es_CR",
            "073b27663394a297eb1c896419b5b5518bd5b345acfe244d00968433ad9709bd",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_CU",
            "3fc1bb0eeffdb99ec8851166453b8d32745da51e9d1036c689f2512dd22c887c",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_DO",
            "194293e1785b2a0ce9b8166cdc4164afebc1ef57db92305786b32350fcb13832",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_EC",
            "caeea1b6bb09c80ca1ff36c81a02ca868fe709f330ac4303806cbf1cd7a6bc0b",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_GT",
            "f424e3546db88d36b38bbc735fd01126296a4f7500ca1368d7017fac561107e7",
            "2cdfcb8d86b70090b3f23f2fe2b6d9d6cfa5f76c803fa8229c674af2b87e6adc",
        ),
        Hunspell(
            "es/es_HN",
            "a1fa7c74a68cb4e7746bae221dd53a27467c320a1cdc2d712cf55926896ee28d",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_MX",
            "44ce35af220962c68f97962776639bef271f7d90a85ba924b539a36e33315f82",
            "d966cb748e4a688ed75ec84b50c0835ac438e5325ee4fce703a093794f9cba7e",
        ),
        Hunspell(
            "es/es_NI",
            "4d7ecc54d369aab904733e64729fffa42413412e14e6a27a32f7fbd56e9161b9",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_PA",
            "bd87e7e25b776d8fe96c4197360909b41d45289ec91c7c47eb000948c04a3ae2",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_PE",
            "1d810a0a319fee329000538cf4df76621bb71228172558b87490f8b9c19b9d75",
            "d966cb748e4a688ed75ec84b50c0835ac438e5325ee4fce703a093794f9cba7e",
        ),
        Hunspell(
            "es/es_PR",
            "11e0815f41a25aac5d6871a13767b1c14a1c6ad5a7c40b359fc4c22f8297ff7f",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_PY",
            "f9a18929f9e5234bc4aba83df1744351156932cb47b77b2ed3358c3170bb41ff",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_SV",
            "3a015f4469046d37806bf374c4d88149cdf570f467a60732f50d736f23d9cac5",
            "dc8213bb8fb08e51dd5869b348d873de23f89e3315656396ca3031fe1d7ffcea",
        ),
        Hunspell(
            "es/es_US",
            "d46932a5c0ec3881fdf265333df4de45a73de51741baedcb5bb54d37b03979c8",
            "674c5a4b4d39fd3b4452f045a4e6e0649db4a2ce23f5903df8c311e21f1a757c",
        ),
        Hunspell(
            "es/es_VE",
            "57eb3aa695c24e1f8f469ac86367b952194c08ee25bc2b761ba4681686ccc723",
            "d966cb748e4a688ed75ec84b50c0835ac438e5325ee4fce703a093794f9cba7e",
        ),
    ),
    credit="(c) Santiago Bosio and others (RLA-ES), GPL-3.0+ or LGPL-3.0+ or MPL-1.1+",
    single_letters=frozenset("aeouy"),
    max_entries=100_000,
    accentless_max_gap=1.0,
)

LANGUAGES["pt_BR"] = Language(
    locale="pt_BR",
    wordfreq="pt",
    hunspell=(
        Hunspell(
            "pt_BR/pt_BR",
            "a38bfb26b68ece2834e79fe83e48d5792652970ace12db89d1b9674bf9933183",
            "21d8ad2a769a60e17e2b5ea4ef11d4d593a58b9e2a82d642ef82d6a4c5523865",
        ),
    ),
    credit="(c) Raimundo Moura and others (VERO), LGPL-3.0 or MPL",
    single_letters=frozenset(["a", "à", "e", "é", "o"]),
    max_entries=100_000,
    accentless_max_gap=1.0,
)

LANGUAGES["de_DE"] = Language(
    locale="de_DE",
    wordfreq="de",
    hunspell=(
        Hunspell(
            "de/de_DE_frami",
            "4ca3c958b0e5545910999bc246f668840bf8ede3df8e5e6790d05edd5a586c38",
            "646bf3333ac69c23e9d794533ee5241d6f755c359e8fe10a648f87613743d594",
        ),
    ),
    credit="(c) Björn Jacke (igerman98) and Franz Michael Baumann, GPL-2.0 or GPL-3.0",
    single_letters=frozenset(),
    max_entries=130_000,
    sharp_s=True,
)


@dataclass
class Word:
    surface: str
    scowl_size: int | None = None
    extra: bool = False
    zipf_override: float | None = None
    flags: set = field(default_factory=set)


def download(url: str, path: Path, sha256: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        print(f"Downloading {url}")
        urllib.request.urlretrieve(url, path)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != sha256:
        raise SystemExit(f"Checksum mismatch for {path.name}: {digest}")
    return path


def download_scowl(cache: Path) -> Path:
    return download(SCOWL_URL, cache / f"scowl-{SCOWL_VERSION}.tar.gz", SCOWL_SHA256)


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
    """Folds a word like Alphabet.kt, or returns None if it doesn't fit the alphabet."""
    chars = []
    for c in word:
        if c in APOSTROPHE_LIKE:
            chars.append("'")
        elif c in HYPHEN_LIKE:
            chars.append("-")
        elif c.lower() in EXPANSIONS:
            chars.append(EXPANSIONS[c.lower()])
        else:
            chars.append(unicodedata.normalize("NFD", c).lower()[:1])
    key = "".join(chars)
    return key if key and set(key) <= ALPHABET else None


def normalize(surface: str) -> str:
    """Writes apostrophes and hyphens the way the keyboard types them."""
    return "".join("'" if c in APOSTROPHE_LIKE else "-" if c in HYPHEN_LIKE else c for c in surface)


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


def apply_deny(words: dict[str, Word], deny: list[list[str]], by_spelling: bool = False):
    """Drops or flags the words of the deny list, matched by folded key, or by lowercase spelling if [by_spelling] so
    that denying ano keeps año."""
    denied = {row[0]: set(row[1]) if len(row) > 1 else {"x"} for row in deny}
    for surface in list(words):
        flags = denied.get(surface.lower() if by_spelling else fold(surface) or surface)
        if flags is None:
            continue
        if "x" in flags:
            del words[surface]
        else:
            words[surface].flags |= flags


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

    apply_deny(words, deny)

    for surface in list(words):
        word = words[surface]
        key = fold(surface)
        short_ok = word.extra or (word.scowl_size is not None and word.scowl_size <= SHORT_WORD_MAX_SIZE)
        if key is not None and len(key) == 1:
            short_ok = word.extra or surface in SINGLE_LETTER_WORDS
        # Letters spelled out when folded (ß, æ, œ) aren't English
        spelled_out = any(c.lower() in EXPANSIONS for c in surface)
        if key is None or spelled_out or (len(key) <= 2 and not short_ok):
            del words[surface]
    return words


def zipf_of(word: Word, lang: str = "en") -> float:
    if word.zipf_override is not None:
        return word.zipf_override
    zipf = zipf_frequency(word.surface, lang)
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
            entries.append(entry_of(word, key, "en"))

    entries.sort(key=lambda e: (e[0], -e[1], e[3] or ""))
    return entries


def entry_of(word: Word, key: str, lang: str) -> tuple[str, int, str, str | None]:
    flags = "".join(sorted(f for f in word.flags if f in "on")) or "-"
    surface = None if word.surface == key else word.surface
    return key, to_freq(zipf_of(word, lang)), flags, surface


def write_tsv(entries, path: Path, locale: str, credits: list[str]):
    header = [
        f"# {locale} word list: {len(entries)} entries, generated by tools/dictionary/build_wordlist.py.",
        *credits,
        f"# Frequencies: wordfreq {version('wordfreq')} (https://github.com/rspeer/wordfreq), (c) Robyn Speer,",
        f"#   data licensed under CC BY-SA 4.0, see LICENSE-{locale}.txt.",
        "# Format: key<TAB>freq<TAB>flags[<TAB>surface]; freq = round(zipf * 30), flags: o = offensive,",
        "#   n = never autocorrect to, - = none.",
    ]
    lines = header + ["\t".join(str(f) for f in entry if f is not None) for entry in entries]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def build_english(cache: Path, output: Path):
    scowl = read_scowl(download_scowl(cache))
    words = collect_words(scowl, read_tsv(HERE / "extras_en.tsv"), read_tsv(HERE / "deny_en.tsv"))
    entries = build_entries(words)
    write_tsv(entries, output, "en_US", [
        f"# Words: SCOWL {SCOWL_VERSION} (http://wordlist.aspell.net/), (c) Kevin Atkinson and others,",
        "#   MIT-like license, see LICENSE-en_US.txt.",
    ])
    return entries


# Hunspell lookups run in worker processes, which load the dictionaries once.
_dictionaries = []
_acronyms: set[str] = set()


def _load_dictionaries(bases: list[str]):
    from spylls.hunspell import Dictionary

    global _dictionaries, _acronyms
    _dictionaries = [Dictionary.from_files(base) for base in bases]
    _acronyms = {w.stem for d in _dictionaries for w in d.dic.words if len(w.stem) > 1 and w.stem.isupper()}


def _accepts(word: str) -> bool:
    return any(d.lookup(word) for d in _dictionaries)


def _spellings(task: tuple[str, bool]) -> list[str]:
    """Returns the spellings of a wordfreq token that Hunspell accepts: lowercase if it's valid, else Title case, else
    ALLCAPS for acronyms. With sharp_s, every ss of the token may also be an ß."""
    token, sharp_s = task
    variants = [token]
    parts = token.split("ss")
    if sharp_s and 1 < len(parts) <= MAX_SHARP_S + 1:
        variants = []
        for joints in product(("ss", "ß"), repeat=len(parts) - 1):
            variants.append("".join(part + joint for part, joint in zip(parts, (*joints, ""))))
    spellings = []
    for variant in variants:
        title = variant[:1].upper() + variant[1:]
        if _accepts(variant):
            spellings.append(variant)
        elif title != variant and _accepts(title):
            spellings.append(title)
        elif variant.upper() in _acronyms:
            spellings.append(variant.upper())
    return spellings


def is_apostrophe_word(word: str, language: Language) -> bool:
    """Whether a word with an apostrophe inside is kept: only in languages that have them, and not when it starts with
    an elided word (l'on, j'devrais), which the keyboard splits off, or with any other single letter (p'pa)."""
    head, _, tail = word.partition("'")
    return language.apostrophe_words > 0 and len(head) > 1 and bool(tail) and head.lower() not in language.elisions


def _apostrophe_stems(bases: list[str], language: Language) -> list[str]:
    """Stems with an apostrophe inside (aujourd'hui, quelqu'un), which wordfreq splits into several tokens."""
    stems = set()
    for base in bases:
        dic = Path(base + ".dic").read_text(encoding="utf-8-sig", errors="replace").splitlines()[1:]
        for line in dic:
            stem = normalize(line.split("/", 1)[0].strip())
            if "'" in stem and "-" not in stem and fold(stem) and is_apostrophe_word(stem, language):
                stems.add(stem)
    return sorted(stems)


def drop_accentless(words: dict[str, "Word"], language: Language):
    """Drops a spelling without accents when a spelling of its key with accents is far more common: people type
    además as ademas so often that wordfreq rates ademas highly, and Hunspell accepts it as a rare verb form (ademar),
    which would stop autocorrect from restoring the accent."""
    if language.accentless_max_gap is None:
        return
    accented: dict[str, float] = {}
    for word in words.values():
        key = fold(word.surface)
        if key != word.surface.lower():
            accented[key] = max(accented.get(key, 0.0), word.zipf_override)
    dropped = []
    for surface in list(words):
        key = fold(surface)
        gap = accented.get(key, 0.0) - words[surface].zipf_override
        if key == surface.lower() and key in accented and gap >= language.accentless_max_gap:
            dropped.append(surface)
            del words[surface]
    print(f"Dropped {len(dropped)} spellings without accents, such as {', '.join(sorted(dropped)[:12])}")


def download_hunspell(language: Language, cache: Path) -> list[str]:
    bases = []
    for source in language.hunspell:
        base = cache / "libreoffice" / LIBREOFFICE_COMMIT / source.path
        download(f"{LIBREOFFICE_URL}/{source.path}.dic", Path(f"{base}.dic"), source.dic_sha256)
        download(f"{LIBREOFFICE_URL}/{source.path}.aff", Path(f"{base}.aff"), source.aff_sha256)
        bases.append(str(base))
    return bases


def check_spellings(language: Language, bases: list[str], tokens: list[str], cache: Path) -> dict[str, list[str]]:
    """Returns the accepted spellings of every token, looked up in parallel and cached between runs."""
    sources = "".join(s.dic_sha256 + s.aff_sha256 for s in language.hunspell)
    digest = hashlib.sha256(f"{sources}{language.sharp_s}".encode()).hexdigest()[:16]
    path = cache / f"spellings-{language.locale}-{digest}.json"
    known: dict[str, list[str]] = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    missing = [t for t in tokens if t not in known]
    if missing:
        print(f"Checking {len(missing)} words with Hunspell")
        with ProcessPoolExecutor(initializer=_load_dictionaries, initargs=(bases,)) as pool:
            tasks = [(t, language.sharp_s) for t in missing]
            for token, spellings in zip(missing, pool.map(_spellings, tasks, chunksize=500)):
                known[token] = spellings
        path.write_text(json.dumps(known, ensure_ascii=False, sort_keys=True), encoding="utf-8")
    return {t: known[t] for t in tokens}


def build_hunspell_language(language: Language, cache: Path, output: Path):
    bases = download_hunspell(language, cache)
    lang = language.wordfreq

    # Candidates: wordfreq tokens down to min_zipf, most frequent first. Clitics (l', qu') are split off in typing.
    frequencies = get_frequency_dict(lang, wordlist="large")
    min_freq = 10 ** (language.min_zipf - 9)
    tokens = [
        t for t, f in frequencies.items()
        if f >= min_freq and fold(t) and ("'" not in t or is_apostrophe_word(t, language))
    ]
    spellings = check_spellings(language, bases, tokens, cache)

    accepted: dict[str, float] = {}
    for token in tokens:
        for surface in spellings[token]:
            accepted.setdefault(normalize(surface), zipf_frequency(surface, lang))

    # wordfreq guesses the frequency of a word it splits from its parts, which overrates rare words made of common
    # parts, so only the most frequent ones are taken. Words with hyphens come from the extras.
    stems = _apostrophe_stems(bases, language)
    joined = sorted(((zipf_frequency(stem, lang), stem) for stem in stems), reverse=True)[:language.apostrophe_words]
    joined = [(zipf, stem) for zipf, stem in joined if zipf >= language.joined_min_zipf]
    joined_spellings = check_spellings(language, bases, [stem for _, stem in joined], cache)
    for zipf, stem in joined:
        for surface in joined_spellings[stem]:
            accepted.setdefault(normalize(surface), zipf)

    words = {s: Word(s, zipf_override=z) for s, z in accepted.items()}
    deny = read_tsv(HERE / f"deny_{language.locale}.tsv")
    apply_deny(words, deny, by_spelling=True)
    for surface in list(words):
        key = fold(surface)
        if len(key) == 1 and surface.lower() not in language.single_letters:
            del words[surface]
        elif len(key) == 2 and words[surface].zipf_override < language.short_min_zipf:
            del words[surface]

    drop_accentless(words, language)

    # The most frequent words, and every other spelling of their keys (sabia next to sábia), so a valid word typed
    # without accents is never taken for a typo of its accented sibling
    ranked = sorted(words.values(), key=lambda w: (-w.zipf_override, w.surface))
    kept = ranked[:language.max_entries]
    kept_keys = {fold(w.surface) for w in kept}
    kept += [w for w in ranked[language.max_entries:] if fold(w.surface) in kept_keys]
    chosen = {w.surface: w for w in kept}

    extras = read_tsv(HERE / f"extras_{language.locale}.tsv")
    for row in extras:
        word = chosen.setdefault(row[0], Word(row[0]))
        word.extra = True
        word.zipf_override = float(row[1]) if len(row) > 1 and row[1] else None
    apply_deny(chosen, deny, by_spelling=True)

    # One spelling per key and way of writing it: the lowercase one if it's valid, as a capital may be the sentence's
    by_spelling: dict[tuple[str, str], list[Word]] = {}
    for word in chosen.values():
        by_spelling.setdefault((fold(word.surface), word.surface.lower()), []).append(word)
    chosen_words = [min(group, key=lambda w: (case_rank(w.surface, key), w.surface))
                    for (key, _), group in by_spelling.items()]

    # An acronym is left out if its key has other spellings, which it would shadow: "so" is "só" rather than "SO"
    spellings_per_key: dict[str, int] = {}
    for word in chosen_words:
        spellings_per_key[fold(word.surface)] = spellings_per_key.get(fold(word.surface), 0) + 1
    entries = []
    for word in chosen_words:
        key = fold(word.surface)
        is_acronym = len(word.surface) > 1 and word.surface.isupper()
        if not (is_acronym and spellings_per_key[key] > 1 and not word.extra):
            entries.append(entry_of(word, key, lang))

    entries.sort(key=lambda e: (e[0], -e[1], e[3] or ""))
    names = ", ".join(Path(s.path).name for s in language.hunspell)
    credit = (
        f"Words: Hunspell dictionaries {names} of LibreOffice (https://github.com/LibreOffice/dictionaries), "
        f"{language.credit}, see LICENSE-{language.locale}.txt."
    )
    lines = textwrap.wrap(credit, HEADER_WIDTH, subsequent_indent="  ", break_on_hyphens=False)
    write_tsv(entries, output, language.locale, [f"# {line}" for line in lines])
    return entries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--lang", default="en_US", choices=["en_US", *LANGUAGES, "all"], help="the locale to build")
    parser.add_argument("--cache", type=Path, default=HERE / ".cache", help="where to keep downloads and lookups")
    parser.add_argument("--output", type=Path, help="the file to write, by default the asset of the locale")
    args = parser.parse_args()

    locales = ["en_US", *LANGUAGES] if args.lang == "all" else [args.lang]
    for locale in locales:
        output = args.output if args.output and len(locales) == 1 else ASSETS / f"{locale}.tsv"
        if locale == "en_US":
            entries = build_english(args.cache, output)
        else:
            entries = build_hunspell_language(LANGUAGES[locale], args.cache, output)
        print(f"Wrote {len(entries)} entries to {output}")


if __name__ == "__main__":
    main()
