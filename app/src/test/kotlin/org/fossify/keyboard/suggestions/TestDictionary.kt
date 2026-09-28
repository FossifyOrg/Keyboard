package org.fossify.keyboard.suggestions

import java.io.File

/**
 * The real dictionary assets, loaded once per locale for all tests, and the word lists they are packed from. Unit tests
 * run with the module as working dir.
 */
object TestDictionary {
    private val loaded = HashMap<String, Pair<List<DictionaryEntry>, ArrayTrie>>()

    fun file(locale: String) = File("src/main/assets/dictionaries/$locale.dict")

    fun wordList(locale: String) = File("../tools/dictionary/wordlists/$locale.tsv")

    fun entries(locale: String) = load(locale).first

    fun trie(locale: String) = load(locale).second

    /** The English dictionary, which most tests use. */
    val entries: List<DictionaryEntry>
        get() = entries(ENGLISH)

    val trie: ArrayTrie
        get() = trie(ENGLISH)

    @Synchronized
    private fun load(locale: String) = loaded.getOrPut(locale) {
        val entries = file(locale).inputStream().use { DictionaryLoader.read(it) }
        entries to TrieBuilder.build(entries)
    }

    private const val ENGLISH = "en_US"
}
