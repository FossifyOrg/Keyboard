package org.fossify.keyboard.suggestions

import java.io.File

/** The real en_US dictionary asset, loaded once for all tests. Unit tests run with the module as working directory. */
object TestDictionary {
    val file = File("src/main/assets/dictionaries/en_US.tsv")

    val entries: List<DictionaryEntry> by lazy { file.inputStream().use { DictionaryLoader.parse(it) } }

    val trie: ArrayTrie by lazy { TrieBuilder.build(entries) }
}
