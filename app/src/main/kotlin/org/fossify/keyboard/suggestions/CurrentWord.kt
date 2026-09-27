package org.fossify.keyboard.suggestions

import java.util.Locale

/**
 * The word being typed: the letters, apostrophes and hyphens right before the cursor. [token] is everything since the
 * last whitespace, so it also holds what comes before the word, like the @ of a mention or the path of a URL.
 */
class CurrentWord(val word: String, val token: String) {
    /**
     * Returns the part of the word that is looked up, or null if there is none yet. A word the dictionary knows, or
     * the start of one, is looked up whole ([isKnown]). Otherwise, as the [rules] of the language say, an elided word
     * and its apostrophe are split off (the l' of l'homme) and so is everything up to the last hyphen (the peut- of
     * peut-être), again until the rest is known or can't be split. A word ending in an apostrophe or a hyphen, like
     * l' or peut-, isn't looked up until more is typed.
     */
    fun lookedUp(rules: LanguageRules, isKnown: (String) -> Boolean): CurrentWord? {
        if (rules.elisionPrefixes.isEmpty() && !rules.splitsAtHyphen) return this
        if (Alphabet.fold(word.last()).let { it == Alphabet.APOSTROPHE || it == Alphabet.HYPHEN }) return null

        var rest = word
        while (!isKnown(rest)) {
            val tail = splitOff(rest, rules) ?: break
            if (tail.isEmpty()) return null
            rest = tail
        }

        return if (rest.length == word.length) this else CurrentWord(rest, token)
    }

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

        /** Returns what follows an elided word or the last hyphen of [word], or null if it has neither. */
        private fun splitOff(word: String, rules: LanguageRules): String? {
            val apostrophe = word.indexOfFirst { Alphabet.fold(it) == Alphabet.APOSTROPHE }
            if (apostrophe > 0 && word.substring(0, apostrophe).lowercase(Locale.ROOT) in rules.elisionPrefixes) {
                return word.substring(apostrophe + 1)
            }

            val hyphen = word.indexOfLast { Alphabet.fold(it) == Alphabet.HYPHEN }
            return if (rules.splitsAtHyphen && hyphen >= 0) word.substring(hyphen + 1) else null
        }

        private fun isWordChar(c: Char): Boolean {
            if (c.isLetter()) return true
            val folded = Alphabet.fold(c)
            return folded == Alphabet.APOSTROPHE || folded == Alphabet.HYPHEN
        }
    }
}
