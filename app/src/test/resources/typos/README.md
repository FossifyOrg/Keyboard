# Typo datasets

Test data for `TypoEvaluationTest` and the weights study of `EngineTuningTest`, one folder per dictionary locale.
Files with typos have lines of `typed<TAB>intended`, files with words have one word per line.

- `real_misspellings.tsv`: well-known spelling mistakes of the language (accomodate, recieve), compiled by hand.
- `mobile_typos.tsv`: slips typical of touch keyboards on the language's layout (thw, yiu, dont), and words typed
  without their accents where the language has them, written by hand.
- `oov_words.txt`: valid words missing from the dictionary (names, brands, slang), which autocorrect should leave
  alone, written by hand.
- `ambiguous_words.txt` (optional): valid words spelled like other words but for their accents (esta and está),
  which autocorrect must never change.

`Evaluation.kt` generates more sets with a fixed seed: keyboard slips of the 5000 most frequent dictionary words,
prefixes of those words to complete, and for languages that restore accents, words typed without them. Every set is
split into a dev half, used to tune the language's `EngineWeights`, and a test half, used for the reported numbers.
