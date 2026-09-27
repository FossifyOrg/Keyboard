# Dictionary pipeline

`build_wordlist.py` generates `app/src/main/assets/dictionaries/en_US.tsv`, the word list behind word suggestions
and autocorrect. The script is run by hand and its output is committed, so the build doesn't need Python or network
access.

## Reproducing

Requires Python 3.11.

```sh
cd tools/dictionary
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python build_wordlist.py
```

The script downloads the SCOWL 2020.12.07 tarball from SourceForge into `.cache/` and checks it against a pinned
SHA-256. With the same wordfreq version, the output is identical.

## Sources

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

## Rules

- Every word must fold (lowercase, accents removed, `’` → `'`) into the alphabet `a–z ' -`.
- One- and two-letter words must come from SCOWL size 35 or below, or from the extras. This removes noise such as
  `tj` and `gp`. The only single letters kept are `a` and `I`.
- When a lowercase spelling is valid, it is the only plain spelling kept: `will` beats `Will`. Otherwise Title case,
  then ALLCAPS, then any other spelling is kept (`Monday`, `TV`, `iPhone`). Lowercase accented spellings (`café`) are
  kept next to it under the same folded key.

## Output format

```
key<TAB>freq<TAB>flags[<TAB>surface]
```

- `key` is the folded word. Lines are sorted by key.
- `freq` is `round(zipf × 30)`, clamped to 1–255.
- `flags` is a combination of `o` and `n`, or `-`.
- `surface` is present when the word isn't written like its key.

Licenses are in `app/src/main/assets/dictionaries/LICENSE-en_US.txt`. SCOWL uses an MIT-like license; wordfreq data is
CC BY-SA 4.0.
