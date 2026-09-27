package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Checks the dictionary of each language against what its [LanguageCase] expects of it. */
@RunWith(Parameterized::class)
class LanguageDictionaryTest(locale: String) {
    private val language = LanguageCase.of(locale)
    private val entries = language.entries
    private val trie = language.trie
    private val engine = language.engine()

    private fun surfacesOf(word: String): List<String> {
        val key = Alphabet.fold(word) ?: return emptyList()
        val node = trie.find(key)
        if (node == ArrayTrie.NO_NODE || !trie.isTerminal(node)) return emptyList()
        val surfaces = ArrayList<String>()
        trie.forEachWord(node, key) { surface, _, _ -> surfaces.add(surface) }
        return surfaces
    }

    @Test
    fun entriesAreSortedUniqueAndFolded() {
        val seen = HashSet<Pair<String, String?>>()
        entries.zipWithNext { a, b -> assertTrue("${a.key} ${b.key}", a.key <= b.key) }
        for (entry in entries) {
            assertEquals(entry.key, Alphabet.fold(entry.key))
            assertEquals(entry.key, Alphabet.fold(entry.surface ?: entry.key))
            assertTrue("${entry.key} ${entry.surface}", seen.add(entry.key to entry.surface))
        }
    }

    @Test
    fun sizeIsExpected() {
        assertTrue("${entries.size} words", entries.size in language.sizes)
    }

    @Test
    fun commonestWordIsFirst() {
        val commonest = entries.maxBy { it.freq }
        assertEquals(language.commonestWord, commonest.surface ?: commonest.key)
    }

    @Test
    fun expectedWordsArePresent() {
        for (word in language.expectedWords) {
            assertTrue("$word in ${surfacesOf(word)}", word in surfacesOf(word))
        }
    }

    @Test
    fun typosAndNoiseAreAbsent() {
        for (word in language.absentWords) {
            assertFalse(word, engine.isWord(word))
        }
    }

    @Test
    fun offensiveWordsAreValidButNeverSuggested() {
        for (word in language.offensiveWords) {
            val entry = entries.first { (it.surface ?: it.key) == word }
            assertTrue(word, entry.flags and ArrayTrie.OFFENSIVE != 0)
            for (prefix in 1..word.length) {
                val typed = word.substring(0, prefix)
                assertFalse(typed, engine.suggest(typed).words.any { it.equals(word, ignoreCase = true) })
            }
        }
    }

    @Test
    fun loadsQuickly() {
        val start = System.nanoTime()
        val loaded = TestDictionary.file(language.locale).inputStream().use { DictionaryLoader.load(it) }
        val millis = (System.nanoTime() - start) / NANOS_PER_MILLI
        val sizes = "${loaded.wordCount} words | ${loaded.nodeCount} nodes"
        println("| ${language.locale} | $sizes | %.0f ms |".format(millis))
        assertTrue("$millis ms", millis < MAX_LOAD_MILLIS)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun locales() = LanguageCase.ALL.map { it.locale }

        private const val NANOS_PER_MILLI = 1e6
        private const val MAX_LOAD_MILLIS = 3000
    }
}
