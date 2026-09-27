package org.fossify.keyboard.suggestions

/**
 * Jaro-Winkler similarity between 0 and 1. It rewards a shared prefix, which matches the observation that the first
 * letters of a word are rarely mistyped. It isn't a metric and can't prune a search, so it only re-ranks candidates.
 */
object JaroWinkler {
    private const val JARO_TERMS = 3f

    fun similarity(a: CharSequence, b: CharSequence): Float {
        if (a.isEmpty() || b.isEmpty()) return if (a.isEmpty() && b.isEmpty()) 1f else 0f

        val matchedA = BooleanArray(a.length)
        val matchedB = BooleanArray(b.length)
        val matches = match(a, b, matchedA, matchedB)
        if (matches == 0) return 0f

        val m = matches.toFloat()
        val halfTranspositions = transpositions(a, b, matchedA, matchedB) / 2f
        val jaro = (m / a.length + m / b.length + (m - halfTranspositions) / m) / JARO_TERMS
        return jaro + commonPrefix(a, b) * EngineConstants.JW_PREFIX_SCALE * (1 - jaro)
    }

    /** Marks the chars of [a] and [b] that are equal and not too far apart, and returns how many pairs there are. */
    private fun match(a: CharSequence, b: CharSequence, matchedA: BooleanArray, matchedB: BooleanArray): Int {
        val window = maxOf(0, maxOf(a.length, b.length) / 2 - 1)
        var matches = 0
        for (i in a.indices) {
            val from = maxOf(0, i - window)
            val to = minOf(b.length - 1, i + window)
            for (j in from..to) {
                if (!matchedB[j] && a[i] == b[j]) {
                    matchedA[i] = true
                    matchedB[j] = true
                    matches++
                    break
                }
            }
        }

        return matches
    }

    /** Counts the matched chars that appear in a different order in [a] and [b]. */
    private fun transpositions(a: CharSequence, b: CharSequence, matchedA: BooleanArray, matchedB: BooleanArray): Int {
        var transpositions = 0
        var j = 0
        for (i in a.indices) {
            if (!matchedA[i]) continue
            while (!matchedB[j]) j++
            if (a[i] != b[j]) transpositions++
            j++
        }

        return transpositions
    }

    private fun commonPrefix(a: CharSequence, b: CharSequence): Int {
        val max = minOf(EngineConstants.JW_MAX_PREFIX, a.length, b.length)
        var prefix = 0
        while (prefix < max && a[prefix] == b[prefix]) prefix++
        return prefix
    }
}
