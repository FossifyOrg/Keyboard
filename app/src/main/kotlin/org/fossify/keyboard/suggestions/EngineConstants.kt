package org.fossify.keyboard.suggestions


/** Tuning constants of the suggestion engine. Costs are in edits, distances in key widths. Weights were tuned with
 * TypoEvaluationTest on the dev half of its datasets.
 *
 * The weights of the error model and the ranking tuned per language (SUB_BASE, SUB_PER_KEY, INS_NEAR, DEL_APOSTROPHE,
 * DEL_DOUBLE, TRANSPOSITION, FIRST_LETTER_FACTOR, COST_WEIGHT, JW_WEIGHT and AUTOCORRECT_MARGIN) are those of English
 * here, and the defaults of [EngineWeights].
 */
object EngineConstants {
    // Substitution: min(SUB_MAX, SUB_BASE + SUB_PER_KEY * distance between the key centres)
    const val SUB_BASE = 0.4f
    const val SUB_PER_KEY = 0.3f
    const val SUB_MAX = 1.0f

    /** Keys whose centres are at most this far apart are neighbours. */
    const val NEIGHBOUR_DISTANCE = 1.25f

    // Insertion: an extra typed char
    const val INS_NEAR = 0.8f
    const val INS_DEFAULT = 1.0f

    // Deletion: a char missing from the typed word
    const val DEL_APOSTROPHE = 0.1f
    const val DEL_DOUBLE = 0.6f
    const val DEL_DEFAULT = 1.0f

    const val TRANSPOSITION = 0.65f

    /** Edits of the first letter are rarer than edits later in the word. */
    const val FIRST_LETTER_FACTOR = 1.15f

    // Maximum correction cost by typed length
    const val MAX_COST_SHORT = 1.0f
    const val MAX_COST_MEDIUM = 1.5f
    const val MAX_COST_LONG = 2.0f
    const val MAX_COST_VERY_LONG = 2.5f
    const val SHORT_WORD_LENGTH = 2
    const val MEDIUM_WORD_LENGTH = 4
    const val LONG_WORD_LENGTH = 7

    /** Candidates may be at most this many chars longer than the typed word. */
    const val MAX_EXTRA_CHARS = 4

    /** Nodes the fuzzy search may visit per query. */
    const val NODE_BUDGET = 40_000

    // Completion of a (possibly misspelled) prefix
    const val MIN_FUZZY_PREFIX_LENGTH = 3
    const val MAX_PREFIX_COST = 1.0f
    const val COMPLETION_COST = 0.6f
    const val COMPLETIONS_PER_PREFIX = 3
    const val COMPLETION_CANDIDATES = 8

    // Ranking: score = freq / FREQ_SCALE - COST_WEIGHT * cost + JW_WEIGHT * jaroWinkler + bonuses
    const val FREQ_SCALE = 30f
    const val COST_WEIGHT = 3.5f
    const val JW_WEIGHT = 6.0f
    const val EXACT_BONUS = 1.0f

    /** Added to the spelling of the typed key that matches the typed accents, above any frequency difference. */
    const val TYPED_SPELLING_BONUS = 9.0f

    // Jaro-Winkler
    const val JW_PREFIX_SCALE = 0.1f
    const val JW_MAX_PREFIX = 4

    // Autocorrect: the best candidate must beat the runner-up by a margin, and the score the typed word would get as
    // a rare word (zipf 1) that isn't in the dictionary. The margin is swept by TypoEvaluationTest.
    const val UNKNOWN_WORD_FREQ = 30
    const val AUTOCORRECT_MARGIN = 0.6f

    // Learned words: the most recent ones kept, and the Zipf frequency they're ranked with on their first use
    const val MAX_USER_WORDS = 2000
    const val USER_WORD_ZIPF = 4.0f

    /**
     * Learning: a word is confirmed each time it's kept as typed, its correction is undone or its typed form is picked
     * on the strip. It's learned and suggested after LEARN_AFTER_USES confirmations, but still corrected like an
     * unknown word until STICKY_USES, so a typo kept by mistake doesn't stick.
     */
    const val LEARN_AFTER_USES = 2
    const val STICKY_USES = 3

    // Compounds: words at least this long made of two words at least this long are valid in some languages
    const val MIN_COMPOUND_LENGTH = 8
    const val MIN_COMPOUND_PART_LENGTH = 3

    const val MAX_SUGGESTIONS = 3
    const val CACHE_SIZE = 32
}
