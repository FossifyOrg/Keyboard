package org.fossify.keyboard.suggestions

import java.text.Normalizer
import java.util.Locale

/**
 * Folds characters into the small symbol alphabet the dictionary trie is built on: lowercase letters without accents,
 * the apostrophe and the hyphen. Symbols are numbered in the same order as their characters, so keys sorted as
 * strings are also sorted by symbol.
 *
 * The letters ß, æ and œ are folded into two letters (ss, ae, oe), the way they are spelled without them, so Straße
 * and cœur are found when typed as strasse and coeur. Folding a single char gives the first of the two.
 */
object Alphabet {
    const val NO_CHAR = '\u0000'
    const val NO_SYMBOL = -1
    const val APOSTROPHE = '\''
    const val HYPHEN = '-'

    private const val CHARS = "'-abcdefghijklmnopqrstuvwxyz"
    private const val FIRST_LETTER_SYMBOL = 2
    private const val FOLD_TABLE_SIZE = 0x250 // Latin-1 and Latin Extended-A/B
    private const val APOSTROPHE_LIKE = "‘’ʼ`´"
    private const val HYPHEN_LIKE = "‐‑"
    private val EXPANSIONS = mapOf('ß' to "ss", 'æ' to "ae", 'œ' to "oe")

    val size = CHARS.length

    private val foldTable = CharArray(FOLD_TABLE_SIZE) { foldUncached(it.toChar()) }
    private val secondTable = CharArray(FOLD_TABLE_SIZE) { secondUncached(it.toChar()) }

    fun charOf(symbol: Int) = CHARS[symbol]

    /** Returns the symbol of an already folded char, or [NO_SYMBOL]. */
    fun symbolOf(folded: Char): Int {
        return when (folded) {
            in 'a'..'z' -> folded - 'a' + FIRST_LETTER_SYMBOL
            APOSTROPHE -> 0
            HYPHEN -> 1
            else -> NO_SYMBOL
        }
    }

    /**
     * Lowercases [c] and strips accents from it. Returns [NO_CHAR] if [c] has no place in the alphabet, and the first
     * letter if it is folded into two.
     */
    fun fold(c: Char): Char {
        return if (c.code < FOLD_TABLE_SIZE) foldTable[c.code] else foldUncached(c)
    }

    /** Folds every char of [word], or returns null if one of them has no place in the alphabet. */
    fun fold(word: CharSequence): String? {
        val folded = StringBuilder(word.length)
        for (c in word) {
            val first = fold(c)
            if (first == NO_CHAR) return null
            folded.append(first)
            val second = secondOf(c)
            if (second != NO_CHAR) folded.append(second)
        }

        return folded.toString()
    }

    fun isWordChar(c: Char) = fold(c) != NO_CHAR

    /** Returns the second letter of a char folded into two, or [NO_CHAR]. */
    private fun secondOf(c: Char): Char {
        return if (c.code < FOLD_TABLE_SIZE) secondTable[c.code] else secondUncached(c)
    }

    private fun foldUncached(c: Char): Char {
        val expansion = EXPANSIONS[c.lowercaseChar()]
        return when {
            c in APOSTROPHE_LIKE -> APOSTROPHE
            c in HYPHEN_LIKE -> HYPHEN
            expansion != null -> expansion[0]
            else -> {
                val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
                    .lowercase(Locale.ROOT)
                    .firstOrNull() ?: NO_CHAR
                if (symbolOf(base) == NO_SYMBOL) NO_CHAR else base
            }
        }
    }

    private fun secondUncached(c: Char) = EXPANSIONS[c.lowercaseChar()]?.get(1) ?: NO_CHAR
}
