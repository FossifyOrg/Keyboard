package org.fossify.keyboard.suggestions

import java.io.InputStream

/**
 * Reads a dictionary in the TSV format written by `tools/dictionary/build_wordlist.py`:
 * `key<TAB>freq<TAB>flags[<TAB>surface]`, sorted by key, with `#` comment lines. Flags are `o` for offensive words
 * and `n` for words that are never autocorrected to, or `-` for none.
 */
object DictionaryLoader {
    private const val FIELD_SEPARATOR = '\t'
    private const val COMMENT_PREFIX = '#'
    private const val FLAG_OFFENSIVE = 'o'
    private const val FLAG_NO_AUTOCORRECT_TO = 'n'
    private const val RADIX = 10

    fun load(input: InputStream): ArrayTrie = TrieBuilder.build(parse(input))

    fun parse(input: InputStream): List<DictionaryEntry> {
        val entries = ArrayList<DictionaryEntry>()
        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                if (line.isNotEmpty() && line[0] != COMMENT_PREFIX) entries.add(parseLine(line))
            }
        }

        return entries
    }

    private fun parseLine(line: String): DictionaryEntry {
        val keyEnd = line.indexOf(FIELD_SEPARATOR)
        val freqEnd = line.indexOf(FIELD_SEPARATOR, keyEnd + 1)
        require(keyEnd > 0 && freqEnd > keyEnd) { "Malformed dictionary line: $line" }
        val flagsEnd = line.indexOf(FIELD_SEPARATOR, freqEnd + 1).let { if (it == -1) line.length else it }

        var freq = 0
        for (i in keyEnd + 1 until freqEnd) {
            freq = freq * RADIX + line[i].digitToInt()
        }

        var flags = 0
        for (i in freqEnd + 1 until flagsEnd) {
            when (line[i]) {
                FLAG_OFFENSIVE -> flags = flags or ArrayTrie.OFFENSIVE
                FLAG_NO_AUTOCORRECT_TO -> flags = flags or ArrayTrie.NO_AUTOCORRECT_TO
            }
        }

        return DictionaryEntry(
            key = line.substring(0, keyEnd),
            freq = freq,
            flags = flags,
            surface = if (flagsEnd < line.length) line.substring(flagsEnd + 1) else null
        )
    }
}
