# Dictionary pipeline

`build_wordlist.py` generates `wordlists/<locale>.tsv`, the word lists behind word suggestions and autocorrect, and
packs each into `app/src/main/assets/dictionaries/<locale>.dict`, the dictionary the app reads. The script is run by
hand and its output is committed, so the build doesn't need Python or network access. The word lists are kept for
review and the packed dictionaries ship, as they are half the size in the APK.

## Reproducing

Requires Python 3.11.

```sh
cd tools/dictionary
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python build_wordlist.py --lang en_US    # or a locale of LANGUAGES, or all
.venv/bin/python build_wordlist.py --lang all --pack-only    # only pack the word lists again
```

The script downloads its sources into `.cache/` and checks each file against a pinned SHA-256: the SCOWL 2020.12.07
tarball from SourceForge for English, and the Hunspell dictionaries of LibreOffice at a pinned commit of
https://github.com/LibreOffice/dictionaries for the other languages. With the same wordfreq and spylls versions, the
output is identical.

Checking the words of another language with Hunspell takes a few minutes, as spylls is a Hunspell written in Python.
The results are cached in `.cache/`, so later runs take seconds.

## English

- **SCOWL 2020.12.07** decides which words are valid. The prebuilt `final/*` lists are used (ISO-8859-1):
  - `english-words` and `american-words` up to size 50, and sizes 55–60 when the word's Zipf frequency is at least 1.5,
  - `english-upper` and `american-upper` up to size 50 (proper nouns such as Monday and English),
  - `english-contractions` up to size 60.

  Abbreviations, hacker slang, roman numerals and possessives (`dog's`) are left out.
- **wordfreq** supplies frequencies (`zipf_frequency(word, "en")`). Words wordfreq doesn't know get Zipf 1.0.
- **`extras_en.tsv`** adds words SCOWL lacks: `'s` contractions (it's, that's), informal words (okay, gonna, lol), chat
  abbreviations (thx, brb), abbreviations often typed without a period (etc, mr, km) and everyday technology words.
  An optional second column overrides the Zipf frequency. The abbreviations are flagged `n` in `deny_en.tsv`, so
  autocorrect leaves them alone but never corrects to them.
- **`deny_en.tsv`** drops or restricts words, matched by folded key. Flag `x` drops the word. `o` marks an offensive word:
  it's accepted when typed but never suggested or corrected to. `n` marks a word that is suggested but never
  corrected to.

Rules:

- One- and two-letter words must come from SCOWL size 35 or below, or from the extras. This removes noise such as
  `tj` and `gp`. The only single letters kept are `a` and `I`.
- When a lowercase spelling is valid, it is the only plain spelling kept: `will` beats `Will`. Otherwise Title case,
  then ALLCAPS, then any other spelling is kept (`Monday`, `TV`, `iPhone`). Lowercase accented spellings (`café`) are
  kept next to it under the same folded key.
- Words with `ß`, `æ` or `œ` are left out.

## Other languages

`LANGUAGES` lists them, with their Hunspell dictionaries and settings.

- **wordfreq** proposes the words: every word of its `large` list for the language with a Zipf frequency of at least
  `min_zipf`, most frequent first. wordfreq lowercases words and spells `ß` as `ss`, so the spelling of a word comes
  from Hunspell:
  - the lowercase spelling if Hunspell accepts it, else the Title case one (German nouns), else the ALLCAPS one if
    the Hunspell dictionary has it in capitals (acronyms). `sie` and `Sie` are one word, `sie`, as the capital may be
    the sentence's.
  - With `sharp_s` (German), every `ss` of a word may also be an `ß`: `strasse` gives `Straße`.
  - With several Hunspell dictionaries (Spanish of Spain and of the Americas), a word is valid if any of them has it.
- **Words with an apostrophe inside** are only kept in languages with `apostrophe_words` (French), and not when they
  start with an elided word (`l'on`, `j'devrais`) or another single letter. Hunspell stems with an apostrophe inside
  (`aujourd'hui`, `quelqu'un`) are added, as wordfreq splits them into several words: the `apostrophe_words` most
  frequent ones with a Zipf frequency of at least `joined_min_zipf`.
  wordfreq guesses the frequency of such a word from its parts, which overrates rare words made of common parts, so
  words with hyphens (`peut-être`, `E-Mail`) come from the extras instead. The keyboard splits elided words off and
  looks up the rest, and does the same after a hyphen.
- **Frequencies** come from wordfreq (`zipf_frequency(word, lang)`), which spells `Masse` and `Maße` alike, so they
  share one.
- **Selection:** the `max_entries` most frequent words are kept, plus every other valid spelling of their folded keys
  (`sabia` next to `sábia`), so a valid word typed without accents is never taken for a typo of its accented sibling.
  Single letters must be in `single_letters`, and two-letter words need a Zipf frequency of at least `short_min_zipf`.
- **`extras_<locale>.tsv`** and **`deny_<locale>.tsv`** work like the English ones, except that the deny list matches
  lowercase spellings rather than folded keys, so denying `ojala` keeps `ojalá`. It also drops rare valid words that
  are almost always another word typed without its accent, which `accentless_max_gap` misses.
- **Spellings without accents** are dropped where `accentless_max_gap` is set, when a spelling of their key with
  accents is at least that much more frequent in Zipf units (10 times for 1.0): people type `además` as `ademas` so
  often that wordfreq rates `ademas` highly, and Hunspell accepts it as a rare verb form (`ademar`), which would stop
  autocorrect from restoring the accent. Common pairs like `esta` and `está` keep both. German doesn't use it, as
  words like `mochte` next to `möchte` are common in their own right.
- **Acronyms** that share their folded key with another spelling are left out (`SO` next to `só`), as a lowercase word
  typed without accents would be taken for them.

## Rules for every language

- Every word must fold into the alphabet `a–z ' -` like `Alphabet.kt` folds it: lowercase, accents removed, `’` → `'`,
  and `ß`, `æ`, `œ` spelled out as `ss`, `ae`, `oe`.

## Output format

The word list:

```
key<TAB>freq<TAB>flags[<TAB>surface]
```

- `key` is the folded word. Lines are sorted by key.
- `freq` is `round(zipf × 30)`, clamped to 1–255.
- `flags` is a combination of `o` and `n`, or `-`.
- `surface` is present when the word isn't written like its key.

The packed dictionary has the same entries in the same order, stored so they compress well, and `DictionaryLoader.kt`
reads it back:

- the header `FKD1` and the number of entries, a big-endian 32-bit int,
- every word: its surface, or its key when it has none, as the key is the folded surface. A word is stored as the
  number of UTF-8 bytes it shares with the previous word, one byte, then the rest of its UTF-8 bytes and a newline,
- the `freq` of every entry, one byte each,
- the flags of every entry, one byte each: 1 for `o`, 2 for `n`.

The frequencies are kept apart from the words, and each word only stores what differs from the previous one, which
makes the dictionaries half the size in the APK. Tests check that each packed dictionary has the entries of its word
list.

Licenses are in `app/src/main/assets/dictionaries/LICENSE-<locale>.txt`. SCOWL uses an MIT-like license; the Hunspell
dictionaries are under the GPL, the LGPL or the MPL, as listed there; wordfreq data is CC BY-SA 4.0. spylls
(MPL-2.0) is only used to build the lists.
