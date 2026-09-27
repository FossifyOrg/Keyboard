package org.fossify.keyboard.suggestions

import java.util.Locale

/**
 * An immutable Array Mapped Trie stored in flat primitive arrays.
 *
 * Nodes are numbered in breadth-first order and the children of a node are stored contiguously. Every node has a
 * bitmap with one bit per child symbol, so the child for a symbol is found by counting the set bits below it. Per node:
 * - `bitmaps`: the child symbol bitmap,
 * - `links`: the index of the first child in the low 24 bits and the word flags in the high 8 bits,
 * - `freqs`: the frequency of the node's own word in the high byte and the highest frequency in its subtree in the
 *   low byte.
 *
 * Surfaces that can't be derived from the folded key and a case pattern (iPhone, café) and any extra surfaces of a key
 * live in a [VariantTable].
 */
class ArrayTrie internal constructor(
    private val bitmaps: LongArray,
    private val links: IntArray,
    private val freqs: ShortArray,
    private val variants: VariantTable,
    val wordCount: Int,
) {
    val nodeCount: Int
        get() = bitmaps.size

    fun childBitmap(node: Int) = bitmaps[node]

    fun firstChild(node: Int) = links[node] and CHILD_MASK

    fun child(node: Int, symbol: Int): Int {
        val bitmap = bitmaps[node]
        val bit = 1L shl symbol
        if (bitmap and bit == 0L) return NO_NODE
        return firstChild(node) + (bitmap and (bit - 1)).countOneBits()
    }

    fun isTerminal(node: Int) = links[node] and FLAG_TERMINAL != 0

    /** Returns the [OFFENSIVE] and [NO_AUTOCORRECT_TO] flags of the node's primary word. */
    fun wordFlags(node: Int) = (links[node] ushr WORD_FLAGS_SHIFT) and WORD_FLAGS_MASK

    fun caseKind(node: Int) = (links[node] ushr CASE_SHIFT) and CASE_MASK

    fun ownFreq(node: Int) = (freqs[node].toInt() ushr Byte.SIZE_BITS) and BYTE_MASK

    fun maxFreq(node: Int) = freqs[node].toInt() and BYTE_MASK

    /** Returns the surface of the node's primary word, given its folded [key]. */
    fun primarySurface(node: Int, key: String): String {
        return when (caseKind(node)) {
            CASE_TITLE -> key.replaceFirstChar { it.titlecase(Locale.ROOT) }
            CASE_UPPER -> key.uppercase(Locale.ROOT)
            CASE_VARIANT -> variants.surface(variants.first(node))
            else -> key
        }
    }

    /** Calls [action] with the surface, frequency and word flags of every word stored under the node. */
    fun forEachWord(node: Int, key: String, action: (surface: String, freq: Int, flags: Int) -> Unit) {
        if (isTerminal(node)) {
            action(primarySurface(node, key), ownFreq(node), wordFlags(node))
        }

        if (links[node] and FLAG_HAS_VARIANT != 0) {
            val skip = if (caseKind(node) == CASE_VARIANT) 1 else 0
            variants.forEach(node, skip, action)
        }
    }

    companion object {
        const val ROOT = 0
        const val NO_NODE = -1

        const val OFFENSIVE = 1
        const val NO_AUTOCORRECT_TO = 2

        const val CASE_LOWER = 0
        const val CASE_TITLE = 1
        const val CASE_UPPER = 2
        const val CASE_VARIANT = 3

        internal const val CHILD_BITS = 24
        internal const val CHILD_MASK = (1 shl CHILD_BITS) - 1
        internal const val FLAG_TERMINAL = 1 shl CHILD_BITS
        internal const val CASE_SHIFT = CHILD_BITS + 1
        internal const val CASE_MASK = 3
        internal const val WORD_FLAGS_SHIFT = CHILD_BITS + 3
        internal const val WORD_FLAGS_MASK = 3
        internal const val FLAG_HAS_VARIANT = 1 shl (CHILD_BITS + 5)
        internal const val BYTE_MASK = 0xFF
        internal const val MAX_FREQ = 255
    }
}

/** Returns the node reached by the folded chars of [word], or [ArrayTrie.NO_NODE]. It isn't necessarily a word. */
fun ArrayTrie.find(word: CharSequence): Int {
    val key = Alphabet.fold(word) ?: return ArrayTrie.NO_NODE
    var node = ArrayTrie.ROOT
    for (c in key) {
        node = child(node, Alphabet.symbolOf(c))
        if (node == ArrayTrie.NO_NODE) return ArrayTrie.NO_NODE
    }

    return node
}

operator fun ArrayTrie.contains(word: CharSequence): Boolean {
    val node = find(word)
    return node != ArrayTrie.NO_NODE && isTerminal(node)
}

/** Extra surfaces of trie nodes, sorted by node so the entries of a node are found with a binary search. */
internal class VariantTable(
    private val nodes: IntArray,
    private val surfaces: Array<String>,
    private val freqs: ByteArray,
    private val flags: ByteArray,
) {
    fun first(node: Int): Int {
        var i = nodes.binarySearch(node)
        while (i > 0 && nodes[i - 1] == node) i--
        return i
    }

    fun surface(index: Int) = surfaces[index]

    fun forEach(node: Int, skip: Int, action: (surface: String, freq: Int, flags: Int) -> Unit) {
        var i = first(node) + skip
        while (i < nodes.size && nodes[i] == node) {
            action(surfaces[i], freqs[i].toInt() and ArrayTrie.BYTE_MASK, flags[i].toInt())
            i++
        }
    }
}
