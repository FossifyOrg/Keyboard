package org.fossify.keyboard.suggestions

/**
 * Decides whether a typed word is replaced when a separator is typed. It stays out of the way unless it is confident:
 * valid words, words with digits or symbols, ALLCAPS words and words the user reverted or taught for good are left
 * alone.
 */
object AutocorrectPolicy {
    private const val BLOCKING_CHARS = ".@#/_:"
    private const val LONE_I = "i"

    /**
     * Returns the word [token] should be corrected to, or null. [token] is the text since the last whitespace, so it
     * includes any symbols before the word [suggestions] were made for. [isStickyUserWord] tells whether the typed word
     * was learned for good; a learned word that isn't sticky yet is suggested, but corrected like an unknown word.
     * [margin] is how much the best candidate must beat the runner-up by.
     */
    fun correctionFor(
        token: String,
        suggestions: Suggestions,
        reverted: Set<String>,
        isStickyUserWord: Boolean,
        margin: Float = EngineConstants.AUTOCORRECT_MARGIN,
    ): ScoredWord? {
        val typed = suggestions.typed
        val candidates = if (suggestions.typedIsWord) {
            suggestions.candidates
        } else {
            // The only exact match of a word missing from the dictionary is the learned word itself
            suggestions.candidates.filterNot { it.isExact }
        }

        val top = candidates.firstOrNull()
        return when {
            top == null || !isCorrectable(token, typed) || typed in reverted || isStickyUserWord -> null
            suggestions.typedIsWord -> top.takeIf { isCaseFix(typed, it) }
            !isSafe(top, suggestions.maxCost) -> null
            isApostropheFix(typed, top) -> top
            else -> top.takeIf { isConfident(it, candidates.getOrNull(1), suggestions.unknownWordScore, margin) }
        }
    }

    /** Words with digits or inside mentions, hashtags, paths, URLs and e-mail addresses are never corrected. */
    fun isCorrectable(token: String, typed: String): Boolean {
        return when {
            typed.isEmpty() || (typed.length < 2 && typed != LONE_I) -> false
            token.any { it.isDigit() || it in BLOCKING_CHARS } -> false
            else -> typed.length < 2 || CaseMapper.patternOf(typed) != CaseMapper.UPPER
        }
    }

    private fun isSafe(candidate: ScoredWord, maxCost: Float): Boolean {
        val blocked = candidate.flags and (ArrayTrie.OFFENSIVE or ArrayTrie.NO_AUTOCORRECT_TO) != 0
        return !blocked && !candidate.isCompletion && candidate.cost <= maxCost
    }

    /** The best candidate must clearly beat the runner-up and the typed word itself as an unknown word. */
    private fun isConfident(top: ScoredWord, runnerUp: ScoredWord?, unknownWordScore: Float, margin: Float): Boolean {
        val runnerUpScore = runnerUp?.score ?: Float.NEGATIVE_INFINITY
        return top.score - runnerUpScore >= margin && top.score > unknownWordScore
    }

    /** dont → don't and im → I'm are applied without a margin, as leaving out apostrophes is so common. */
    private fun isApostropheFix(typed: String, top: ScoredWord): Boolean {
        return top.word.replace(Alphabet.APOSTROPHE.toString(), "").equals(typed, ignoreCase = true)
    }

    /** i → I and monday → Monday are applied even though the typed word is valid. */
    private fun isCaseFix(typed: String, top: ScoredWord): Boolean {
        return top.isExact && top.word != typed && top.word.equals(typed, ignoreCase = true)
    }
}
