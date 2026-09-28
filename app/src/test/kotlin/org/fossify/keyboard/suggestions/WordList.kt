package org.fossify.keyboard.suggestions

import java.io.InputStream

/**
 * Reads a word list in the TSV format written by `tools/dictionary/build_wordlist.py`, which is packed into the
 * dictionary the app reads: `key<TAB>freq<TAB>flags[<TAB>surface]`, sorted by key, with `#` comment lines. Flags are
 * `o` for offensive words and `n` for words that are never autocorrected to, or `-` for none.
 */
object WordList {
    private const val FIELD_SEPARATOR = '\t'
    private const val COMMENT_PREFIX = '#'
    private const val FLAG_OFFENSIVE = 'o'
    private const val FLAG_NO_AUTOCORRECT_TO = 'n'

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
        val fields = line.split(FIELD_SEPARATOR)
        require(fields.size in 3..4) { "Malformed word list line: $line" }
        var flags = 0
        for (c in fields[2]) {
            when (c) {
                FLAG_OFFENSIVE -> flags = flags or ArrayTrie.OFFENSIVE
                FLAG_NO_AUTOCORRECT_TO -> flags = flags or ArrayTrie.NO_AUTOCORRECT_TO
            }
        }

        return DictionaryEntry(key = fields[0], freq = fields[1].toInt(), flags = flags, surface = fields.getOrNull(3))
    }
}
