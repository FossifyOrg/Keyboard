# Typo datasets

Test data for `TypoEvaluationTest`. Each file has lines of `typed<TAB>intended`.

- `real_misspellings.tsv`: well-known English spelling mistakes (accomodate, recieve), compiled by hand.
- `mobile_typos.tsv`: slips typical of touch keyboards (thw, yiu, dont), written by hand.

`Evaluation.kt` generates two more sets with a fixed seed: keyboard slips of the 5000 most frequent dictionary words,
and prefixes of those words to complete. Every set is split into a dev half, used to tune `EngineConstants`, and a
test half, used for the reported numbers.
