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

        fun forLocale(locale: String): LanguageRules {
            return when (locale) {
                "en_US" -> ENGLISH
                else -> DEFAULT
            }
        }
    }
}
