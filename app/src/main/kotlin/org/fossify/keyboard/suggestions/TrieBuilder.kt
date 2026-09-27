package org.fossify.keyboard.suggestions

import java.util.Locale

/**
 * A dictionary word: its folded [key], a frequency byte (zipf × 30), [ArrayTrie.OFFENSIVE] and
 * [ArrayTrie.NO_AUTOCORRECT_TO] flags and a [surface] when the word isn't written like its key.
 */
data class DictionaryEntry(val key: String, val freq: Int, val flags: Int = 0, val surface: String? = null)

/**
 * Builds an [ArrayTrie] in one breadth-first pass over entries sorted by key. Every node is a range of keys sharing
 * a prefix of length `depth`, and nodes are numbered in the order they are queued, so the queue itself is indexed by
 * node and no per-node objects are allocated.
 */
object TrieBuilder {
    fun build(entries: List<DictionaryEntry>): ArrayTrie = Builder(entries).build()

    private class Builder(private val entries: List<DictionaryEntry>) {
        private val groupStarts = groupByKey(entries)
        private val keys = Array(groupStarts.size - 1) { entries[groupStarts[it]].key }
        private val maxNodes = countNodes(keys)

        private val rangeLo = IntArray(maxNodes)
        private val rangeHi = IntArray(maxNodes)
        private val depths = IntArray(maxNodes)
        private val bitmaps = LongArray(maxNodes)
        private val links = IntArray(maxNodes)
        private val ownFreqs = IntArray(maxNodes)
        private var nodeCount = 1

        private val variantNodes = ArrayList<Int>()
        private val variantEntries = ArrayList<DictionaryEntry>()

        fun build(): ArrayTrie {
            rangeHi[ArrayTrie.ROOT] = keys.size
            var node = 0
            while (node < nodeCount) {
                visit(node++)
            }

            return ArrayTrie(
                bitmaps = bitmaps.copyOf(nodeCount),
                links = links.copyOf(nodeCount),
                freqs = packFreqs(),
                variants = VariantTable(
                    nodes = variantNodes.toIntArray(),
                    surfaces = Array(variantEntries.size) { variantEntries[it].surface ?: variantEntries[it].key },
                    freqs = ByteArray(variantEntries.size) { clampFreq(variantEntries[it].freq).toByte() },
                    flags = ByteArray(variantEntries.size) { wordFlagsOf(variantEntries[it]).toByte() },
                ),
                wordCount = entries.size,
            )
        }

        private fun visit(node: Int) {
            val depth = depths[node]
            var lo = rangeLo[node]
            val hi = rangeHi[node]
            if (lo < hi && keys[lo].length == depth) {
                makeTerminal(node, groupStarts[lo] until groupStarts[lo + 1])
                lo++
            }

            if (lo < hi) {
                links[node] = links[node] or nodeCount
            }

            var previousSymbol = Alphabet.NO_SYMBOL
            for (i in lo until hi) {
                val symbol = Alphabet.symbolOf(keys[i][depth])
                require(symbol != Alphabet.NO_SYMBOL) { "Unsupported character in \"${keys[i]}\"" }
                if (symbol != previousSymbol) {
                    if (previousSymbol != Alphabet.NO_SYMBOL) rangeHi[nodeCount - 1] = i
                    bitmaps[node] = bitmaps[node] or (1L shl symbol)
                    rangeLo[nodeCount] = i
                    depths[nodeCount] = depth + 1
                    nodeCount++
                    previousSymbol = symbol
                }
            }

            if (previousSymbol != Alphabet.NO_SYMBOL) rangeHi[nodeCount - 1] = hi
        }

        /** The most frequent entry of a key is its primary word, the others go to the variant table. */
        private fun makeTerminal(node: Int, group: IntRange) {
            val primary = entries[group.maxBy { entries[it].freq }]
            val caseKind = caseKindOf(primary.key, primary.surface)
            var link = ArrayTrie.FLAG_TERMINAL or
                (caseKind shl ArrayTrie.CASE_SHIFT) or
                (wordFlagsOf(primary) shl ArrayTrie.WORD_FLAGS_SHIFT)

            if (caseKind == ArrayTrie.CASE_VARIANT) {
                addVariant(node, primary)
            }

            for (i in group) {
                if (entries[i] !== primary) addVariant(node, entries[i])
            }

            if (caseKind == ArrayTrie.CASE_VARIANT || group.first != group.last) {
                link = link or ArrayTrie.FLAG_HAS_VARIANT
            }

            links[node] = link
            ownFreqs[node] = clampFreq(primary.freq)
        }

        private fun addVariant(node: Int, entry: DictionaryEntry) {
            variantNodes.add(node)
            variantEntries.add(entry)
        }

        /** Children always come after their parent, so a reverse pass sees every subtree before its root. */
        private fun packFreqs(): ShortArray {
            val maxFreqs = ownFreqs.copyOf(nodeCount)
            for (node in nodeCount - 1 downTo 0) {
                val first = links[node] and ArrayTrie.CHILD_MASK
                for (i in 0 until bitmaps[node].countOneBits()) {
                    maxFreqs[node] = maxOf(maxFreqs[node], maxFreqs[first + i])
                }
            }

            return ShortArray(nodeCount) { ((ownFreqs[it] shl Byte.SIZE_BITS) or maxFreqs[it]).toShort() }
        }
    }

    /** Every key adds one node per char after the prefix it shares with the previous key. */
    private fun countNodes(keys: Array<String>): Int {
        var count = 1
        for (i in keys.indices) {
            val common = if (i == 0) 0 else keys[i].commonPrefixWith(keys[i - 1]).length
            count += keys[i].length - common
        }

        return count
    }

    /** Returns the start index of every run of equal keys, followed by `entries.size`. */
    private fun groupByKey(entries: List<DictionaryEntry>): IntArray {
        val starts = ArrayList<Int>()
        for (i in entries.indices) {
            val cmp = if (i == 0) 1 else entries[i].key.compareTo(entries[i - 1].key)
            require(cmp >= 0) { "Dictionary entries must be sorted by key: \"${entries[i].key}\"" }
            if (cmp > 0) starts.add(i)
        }

        starts.add(entries.size)
        return starts.toIntArray()
    }

    private fun caseKindOf(key: String, surface: String?): Int {
        return when (surface) {
            null, key -> ArrayTrie.CASE_LOWER
            key.replaceFirstChar { it.titlecase(Locale.ROOT) } -> ArrayTrie.CASE_TITLE
            key.uppercase(Locale.ROOT) -> ArrayTrie.CASE_UPPER
            else -> ArrayTrie.CASE_VARIANT
        }
    }

    private fun clampFreq(freq: Int) = freq.coerceIn(1, ArrayTrie.MAX_FREQ)

    private fun wordFlagsOf(entry: DictionaryEntry) = entry.flags and ArrayTrie.WORD_FLAGS_MASK
}
