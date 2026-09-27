package org.fossify.keyboard.suggestions

/** What differs between the languages of the dictionaries besides the words: tuned weights and rules of spelling. */
data class LanguageRules(
    val weights: EngineWeights = EngineWeights.ENGLISH,

    /** Single letters that may be corrected, like the English i → I. Other single letters are left alone. */
    val loneLetterFixes: Set<String> = emptySet(),

    /**
     * Whether a word typed without the accents of its only spellings is misspelled (nao → não). Otherwise every word
     * with a dictionary key is valid, so the English cafe is left alone.
     */
    val restoresAccents: Boolean = false,

    /** Elided words written before an apostrophe, like the l of l'homme. The word after them is looked up alone. */
    val elisionPrefixes: Set<String> = emptySet(),

    /** Whether the part after a hyphen is looked up alone when the whole word is unknown, like être in peut-être. */
    val splitsAtHyphen: Boolean = false,

    /** Whether a long word made of two dictionary words, like a German compound, is valid. */
    val compounds: Boolean = false,

    /** Whether a learned word keeps its capital when it's used in lowercase, as German nouns are capitalized. */
    val keepsLearnedCapitals: Boolean = false,

    /** Chars besides the usual separators that finish a word, like closing guillemets. */
    val extraSeparators: String = "",
) {
    companion object {
        val ENGLISH = LanguageRules(loneLetterFixes = setOf("i"))

        /** The rules of dictionaries without rules of their own. */
        val DEFAULT = LanguageRules()

        /** Closing quotes and the ellipsis, which end a word in languages that use them. */
        private const val CLOSING_PUNCTUATION = "»”…"

        /** Tuned by the weights study on the Spanish datasets. */
        val SPANISH = LanguageRules(
            weights = EngineWeights(
                subBase = 0.32f, subPerKey = 0.32f, insNear = 0.78f, delApostrophe = 0.06f, delDouble = 0.71f,
                transposition = 0.78f, firstLetterFactor = 1.23f, costWeight = 3.04f, jwWeight = 6.06f, margin = 0.8f,
            ),
            restoresAccents = true,
            splitsAtHyphen = true,
            extraSeparators = CLOSING_PUNCTUATION,
        )

        /** Tuned by the weights study on the Brazilian Portuguese datasets. */
        val PORTUGUESE = LanguageRules(
            weights = EngineWeights(
                subBase = 0.1f, subPerKey = 0.47f, insNear = 0.44f, delApostrophe = 0.26f, delDouble = 0.33f,
                transposition = 0.44f, firstLetterFactor = 1.24f, costWeight = 2.84f, jwWeight = 8.92f, margin = 1.0f,
            ),
            restoresAccents = true,
            splitsAtHyphen = true,
            extraSeparators = CLOSING_PUNCTUATION,
        )

        /** Tuned by the weights study on the German datasets. German also closes quotes with “, as in „Hallo“. */
        val GERMAN = LanguageRules(
            weights = EngineWeights(
                subBase = 0.39f, subPerKey = 0.3f, insNear = 0.78f, delApostrophe = 0.07f, delDouble = 0.59f,
                transposition = 0.66f, firstLetterFactor = 1.11f, costWeight = 3.14f, jwWeight = 6.68f, margin = 0.6f,
            ),
            restoresAccents = true,
            splitsAtHyphen = true,
            compounds = true,
            keepsLearnedCapitals = true,
            extraSeparators = "$CLOSING_PUNCTUATION“",
        )

        /** Tuned by the weights study on the French datasets. */
        val FRENCH = LanguageRules(
            weights = EngineWeights(
                subBase = 0.32f, subPerKey = 0.35f, insNear = 0.74f, delApostrophe = 0.06f, delDouble = 0.62f,
                transposition = 0.7f, firstLetterFactor = 1.27f, costWeight = 2.81f, jwWeight = 7.79f, margin = 0.8f,
            ),
            restoresAccents = true,
            elisionPrefixes = setOf(
                "c", "d", "j", "l", "m", "n", "qu", "s", "t", "jusqu", "lorsqu", "puisqu", "quoiqu",
            ),
            splitsAtHyphen = true,
            extraSeparators = CLOSING_PUNCTUATION,
        )

        fun forLocale(locale: String): LanguageRules {
            return when (locale) {
                "en_US" -> ENGLISH
                "es" -> SPANISH
                "pt_BR" -> PORTUGUESE
                "de_DE" -> GERMAN
                "fr_FR" -> FRENCH
                else -> DEFAULT
            }
        }
    }
}
