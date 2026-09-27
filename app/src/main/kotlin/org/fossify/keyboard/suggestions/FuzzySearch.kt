package org.fossify.keyboard.suggestions

import java.util.PriorityQueue

/** A dictionary node found for a typed word, with the cost of the edits between them. */
class Candidate(val key: String, val node: Int, val cost: Float, val isCompletion: Boolean)

/**
 * Searches an [ArrayTrie] for the words a typed word could have been meant as.
 *
 * Corrections come from a depth-first walk that keeps one row of a weighted optimal string alignment (restricted
 * Damerau-Levenshtein) matrix per depth, the row two levels up being needed for transpositions. A subtree is skipped
 * once every cell of its row is over the cost limit, and a node budget bounds the worst case. Nodes whose row ends
 * cheaply are misspelled prefixes of longer words; their best completions are harvested afterwards.
 */
class FuzzySearch(private val trie: ArrayTrie) {

    /** Returns up to [limit] of the most frequent words starting at [start], most frequent first. */
    fun completions(start: Int, prefix: String, limit: Int, cost: Float = 0f): List<Candidate> {
        val results = ArrayList<Candidate>(limit)
        val queue = PriorityQueue<QueueItem>(compareByDescending { it.freq })
        queue.add(QueueItem(start, prefix, trie.maxFreq(start), isWord = false))
        while (results.size < limit && queue.isNotEmpty()) {
            val item = queue.poll()!!
            if (item.isWord) {
                results.add(Candidate(item.key, item.node, cost, isCompletion = true))
                continue
            }

            val node = item.node
            if (node != start && trie.isTerminal(node) && trie.wordFlags(node) and ArrayTrie.OFFENSIVE == 0) {
                queue.add(QueueItem(node, item.key, trie.ownFreq(node), isWord = true))
            }

            var bitmap = trie.childBitmap(node)
            var child = trie.firstChild(node)
            while (bitmap != 0L) {
                val key = item.key + Alphabet.charOf(bitmap.countTrailingZeroBits())
                queue.add(QueueItem(child, key, trie.maxFreq(child), isWord = false))
                bitmap = bitmap and (bitmap - 1)
                child++
            }
        }

        return results
    }

    /**
     * Returns the words within [maxCost] of the [typed] symbols, and when [withCompletions] is set the completions of
     * prefixes within [EngineConstants.MAX_PREFIX_COST].
     */
    fun corrections(typed: IntArray, model: ErrorModel, maxCost: Float, withCompletions: Boolean): List<Candidate> {
        val walk = Walk(typed, model, maxCost, withCompletions)
        walk.visit(ArrayTrie.ROOT, 0)
        val results = walk.results
        walk.prefixes.sortedWith(compareBy<Candidate> { it.cost }.thenByDescending { trie.maxFreq(it.node) })
            .take(EngineConstants.COMPLETION_CANDIDATES)
            .forEach { prefix ->
                val cost = prefix.cost + EngineConstants.COMPLETION_COST
                results.addAll(completions(prefix.node, prefix.key, EngineConstants.COMPLETIONS_PER_PREFIX, cost))
            }

        return results
    }

    private inner class Walk(
        private val typed: IntArray,
        private val model: ErrorModel,
        private val maxCost: Float,
        private val withCompletions: Boolean,
    ) {
        private val columns = typed.size + 1
        private val maxDepth = typed.size + EngineConstants.MAX_EXTRA_CHARS
        private val rows = FloatArray((maxDepth + 1) * columns)
        private val path = IntArray(maxDepth + 1)
        private val keyChars = CharArray(maxDepth)
        private val insertions = FloatArray(typed.size) {
            model.insertion(typed, it) * if (it == 0) model.firstLetterFactor else 1f
        }
        private var budget = EngineConstants.NODE_BUDGET

        val results = ArrayList<Candidate>()
        val prefixes = ArrayList<Candidate>()

        init {
            for (i in 1 until columns) {
                rows[i] = rows[i - 1] + insertions[i - 1]
            }
        }

        fun visit(node: Int, depth: Int) {
            var bitmap = trie.childBitmap(node)
            var child = trie.firstChild(node)
            while (bitmap != 0L && budget > 0) {
                budget--
                val symbol = bitmap.countTrailingZeroBits()
                val rowMin = fillRow(depth + 1, symbol)
                if (rowMin <= maxCost) {
                    keyChars[depth] = Alphabet.charOf(symbol)
                    collect(child, depth + 1)
                    if (depth + 1 < maxDepth) visit(child, depth + 1)
                }

                bitmap = bitmap and (bitmap - 1)
                child++
            }
        }

        private fun collect(node: Int, depth: Int) {
            val cost = rows[depth * columns + typed.size]
            if (cost > maxCost) return
            if (trie.isTerminal(node)) {
                results.add(Candidate(String(keyChars, 0, depth), node, cost, isCompletion = false))
            }

            if (withCompletions && isCompletablePrefix(depth, cost)) {
                prefixes.add(Candidate(String(keyChars, 0, depth), node, cost, isCompletion = true))
            }
        }

        /** Short typed words are only completed exactly, longer ones also when they are slightly misspelled. */
        private fun isCompletablePrefix(depth: Int, cost: Float): Boolean {
            val longEnough = typed.size >= EngineConstants.MIN_FUZZY_PREFIX_LENGTH || cost == 0f
            return longEnough && cost <= EngineConstants.MAX_PREFIX_COST && depth >= typed.size - 1
        }

        /** Fills the matrix row for the candidate prefix of length [depth] ending in [symbol]; returns its minimum. */
        private fun fillRow(depth: Int, symbol: Int): Float {
            path[depth] = symbol
            val previousSymbol = if (depth > 1) path[depth - 1] else Alphabet.NO_SYMBOL
            val row = depth * columns
            val above = row - columns
            val deletionFactor = if (depth == 1) model.firstLetterFactor else 1f
            val deletion = model.deletion(symbol, previousSymbol) * deletionFactor

            rows[row] = rows[above] + deletion
            var rowMin = rows[row]
            for (i in 1 until columns) {
                val typedSymbol = typed[i - 1]
                val factor = if (i == 1) model.firstLetterFactor else 1f
                val substitution = if (typedSymbol == symbol) 0f else model.substitution(typed, i - 1, symbol) * factor
                var cost = rows[above + i - 1] + substitution
                cost = minOf(cost, rows[row + i - 1] + insertions[i - 1])
                cost = minOf(cost, rows[above + i] + deletion)
                if (i > 1 && depth > 1 && isTransposition(typed[i - 2], typedSymbol, previousSymbol, symbol)) {
                    cost = minOf(cost, rows[above - columns + i - 2] + model.transposition)
                }

                rows[row + i] = cost
                rowMin = minOf(rowMin, cost)
            }

            return rowMin
        }
    }

    /** Whether the typed pair `ab` is the intended pair `ba`. */
    private fun isTransposition(typedA: Int, typedB: Int, intendedA: Int, intendedB: Int): Boolean {
        return typedA == intendedB && typedB == intendedA && typedA != typedB
    }

    private class QueueItem(val node: Int, val key: String, val freq: Int, val isWord: Boolean)
}
