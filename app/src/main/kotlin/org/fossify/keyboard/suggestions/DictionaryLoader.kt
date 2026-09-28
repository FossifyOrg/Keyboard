package org.fossify.keyboard.suggestions

import java.io.InputStream

/**
 * Reads a dictionary packed by `tools/dictionary/build_wordlist.py` from its word list:
 * - the header `FKD1` and the number of entries, a big-endian int,
 * - every word, sorted by key: the number of UTF-8 bytes it shares with the previous word, a byte, then the rest of its
 *   UTF-8 bytes and a newline. A word is its surface, or its key when it has none, as the key is the folded word,
 * - the frequency byte of every entry,
 * - the flags byte of every entry: 1 for offensive words and 2 for words that are never autocorrected to.
 */
object DictionaryLoader {
    private val HEADER = "FKD1".toByteArray(Charsets.US_ASCII)
    private const val WORD_END = '\n'.code.toByte()
    private const val PACKED_OFFENSIVE = 1
    private const val PACKED_NO_AUTOCORRECT_TO = 2
    private const val BYTE_MASK = 0xFF
    private const val WORD_CAPACITY = 64

    fun load(input: InputStream): ArrayTrie = TrieBuilder.build(read(input))

    fun read(input: InputStream): List<DictionaryEntry> = Reader(input.readBytes()).read()

    private class Reader(private val data: ByteArray) {
        private var position = 0

        fun read(): List<DictionaryEntry> {
            for (b in HEADER) {
                require(next() == b) { "Not a packed dictionary" }
            }

            var count = 0
            repeat(Int.SIZE_BYTES) { count = (count shl Byte.SIZE_BITS) or unsigned(next()) }
            require(count in 0..data.size) { "Malformed packed dictionary" }

            val words = readWords(count)
            val freqs = position
            val flags = freqs + count
            require(flags + count == data.size) { "Malformed packed dictionary" }

            return List(count) { i ->
                val word = words[i]
                val isKey = isFolded(word)
                val key = if (isKey) word else Alphabet.fold(word)
                requireNotNull(key) { "Unsupported character in \"$word\"" }
                DictionaryEntry(
                    key = key,
                    freq = unsigned(data[freqs + i]),
                    flags = flagsOf(unsigned(data[flags + i])),
                    surface = if (isKey) null else word,
                )
            }
        }

        /** Most words are written like their key, and folding them would only make a copy. */
        private fun isFolded(word: String) = word.all { Alphabet.symbolOf(it) != Alphabet.NO_SYMBOL }

        private fun readWords(count: Int): Array<String> {
            var word = ByteArray(WORD_CAPACITY)
            var length = 0
            return Array(count) {
                val shared = unsigned(next())
                require(shared <= length) { "Malformed packed dictionary" }
                val end = indexOfWordEnd()
                val restLength = end - position
                if (shared + restLength > word.size) word = word.copyOf(shared + restLength)
                data.copyInto(word, shared, position, end)
                length = shared + restLength
                position = end + 1
                String(word, 0, length, Charsets.UTF_8)
            }
        }

        private fun indexOfWordEnd(): Int {
            var i = position
            while (i < data.size && data[i] != WORD_END) i++
            require(i < data.size) { "Truncated packed dictionary" }
            return i
        }

        private fun next(): Byte {
            require(position < data.size) { "Truncated packed dictionary" }
            return data[position++]
        }

        private fun flagsOf(packed: Int): Int {
            var flags = 0
            if (packed and PACKED_OFFENSIVE != 0) flags = flags or ArrayTrie.OFFENSIVE
            if (packed and PACKED_NO_AUTOCORRECT_TO != 0) flags = flags or ArrayTrie.NO_AUTOCORRECT_TO
            return flags
        }

        private fun unsigned(b: Byte) = b.toInt() and BYTE_MASK
    }
}
