package org.fossify.keyboard.suggestions

/**
 * The word being typed: the letters, apostrophes and hyphens right before the cursor. [token] is everything since the
 * last whitespace, so it also holds what comes before the word, like the @ of a mention or the path of a URL.
 */
class CurrentWord(val word: String, val token: String) {
    companion object {
        /** How much text before the cursor is read to find the word. Longer words get no suggestions. */
        const val CONTEXT_LENGTH = 48
        const val MAX_LENGTH = 32

        /**
         * Returns the word that ends right before the cursor, given the text [before] and [after] it, or null if there
         * is none or the cursor is inside a word.
         */
        fun find(before: CharSequence, after: CharSequence): CurrentWord? {
            if (after.isNotEmpty() && isWordChar(after[0])) return null

            var start = before.length
            while (start > 0 && isWordChar(before[start - 1])) start--

            // Leading apostrophes and hyphens are quotes and dashes rather than part of the word
            while (start < before.length && !before[start].isLetter()) start++

            val length = before.length - start
            if (length == 0 || length > MAX_LENGTH) return null

            var tokenStart = start
            while (tokenStart > 0 && !before[tokenStart - 1].isWhitespace()) tokenStart--
            return CurrentWord(before.substring(start), before.substring(tokenStart))
        }

        private fun isWordChar(c: Char): Boolean {
            if (c.isLetter()) return true
            val folded = Alphabet.fold(c)
            return folded == Alphabet.APOSTROPHE || folded == Alphabet.HYPHEN
        }
    }
}
