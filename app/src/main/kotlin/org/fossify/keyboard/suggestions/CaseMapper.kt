package org.fossify.keyboard.suggestions

import java.util.Locale

/** Applies the case pattern of what was typed to a candidate written in its dictionary case. */
object CaseMapper {
    const val LOWER = 0
    const val TITLE = 1
    const val UPPER = 2
    const val MIXED = 3

    fun patternOf(typed: CharSequence): Int {
        var upper = 0
        var letters = 0
        for (c in typed) {
            if (c.isLetter()) {
                letters++
                if (c.isUpperCase()) upper++
            }
        }

        return when {
            upper == 0 -> LOWER
            upper == letters && letters > 1 -> UPPER
            upper == 1 && typed.first { it.isLetter() }.isUpperCase() -> TITLE
            else -> MIXED
        }
    }

    /**
     * Lowercase input keeps the dictionary case (i → I, monday → Monday, iphone → iPhone), Title case input capitalises
     * the first letter and ALLCAPS input uppercases the candidate.
     */
    fun apply(candidate: String, pattern: Int): String {
        return when (pattern) {
            TITLE -> if (candidate.hasInnerCapital()) candidate else candidate.capitalized()
            UPPER -> candidate.uppercase(Locale.ROOT)
            else -> candidate
        }
    }

    private fun String.hasInnerCapital() = drop(1).any { it.isUpperCase() }

    private fun String.capitalized() = replaceFirstChar { it.titlecase(Locale.ROOT) }
}
