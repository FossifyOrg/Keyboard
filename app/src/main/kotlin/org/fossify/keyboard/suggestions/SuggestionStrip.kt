package org.fossify.keyboard.suggestions

/**
 * A word on the suggestion strip. [isTyped] marks the word as typed, which autocorrect is about to replace with the
 * chip marked [isCorrection].
 */
data class SuggestionChip(val word: String, val isTyped: Boolean = false, val isCorrection: Boolean = false)

/** Lays out suggestions on the strip: the best word in the middle, the second left and the third right. */
object SuggestionStrip {
    const val SIZE = 3
    const val LEFT = 0
    const val MIDDLE = 1
    const val RIGHT = 2

    /**
     * Returns [SIZE] chips, some of which may be empty, or an empty list if there is nothing to show. A pending
     * [correction] goes in the middle, with the typed word on the left so it can be kept.
     */
    fun arrange(suggestions: Suggestions, correction: ScoredWord? = null): List<SuggestionChip?> {
        if (correction != null) {
            val other = suggestions.candidates.firstOrNull {
                it.word != correction.word && it.word != suggestions.typed
            }

            return listOf(
                SuggestionChip(suggestions.typed, isTyped = true),
                SuggestionChip(correction.word, isCorrection = true),
                other?.let { SuggestionChip(it.word) }
            )
        }

        val words = suggestions.words
        if (words.isEmpty()) return emptyList()

        val chips = arrayOfNulls<SuggestionChip>(SIZE)
        chips[MIDDLE] = SuggestionChip(words[0])
        chips[LEFT] = words.getOrNull(1)?.let { SuggestionChip(it) }
        chips[RIGHT] = words.getOrNull(2)?.let { SuggestionChip(it) }
        return chips.toList()
    }
}
