package org.fossify.keyboard.suggestions

import java.text.Normalizer
import java.util.Locale

/**
 * Folds characters into the small symbol alphabet the dictionary trie is built on: lowercase letters without accents,
 * the apostrophe and the hyphen. Symbols are numbered in the same order as their characters, so keys sorted as
 * strings are also sorted by symbol.
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

    val size = CHARS.length

    private val foldTable = CharArray(FOLD_TABLE_SIZE) { foldUncached(it.toChar()) }

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

    /** Lowercases [c] and strips accents from it. Returns [NO_CHAR] if [c] has no place in the alphabet. */
    fun fold(c: Char): Char {
        return if (c.code < FOLD_TABLE_SIZE) foldTable[c.code] else foldUncached(c)
    }

    /** Folds every char of [word], or returns null if one of them has no place in the alphabet. */
    fun fold(word: CharSequence): String? {
        val chars = CharArray(word.length)
        for (i in word.indices) {
            val folded = fold(word[i])
            if (folded == NO_CHAR) return null
            chars[i] = folded
        }

        return String(chars)
    }

    fun isWordChar(c: Char) = fold(c) != NO_CHAR

    private fun foldUncached(c: Char): Char {
        return when {
            c in APOSTROPHE_LIKE -> APOSTROPHE
            c in HYPHEN_LIKE -> HYPHEN
            else -> {
                val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
                    .lowercase(Locale.ROOT)
                    .firstOrNull() ?: NO_CHAR
                if (symbolOf(base) == NO_SYMBOL) NO_CHAR else base
            }
        }
    }
}
