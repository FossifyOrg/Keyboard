package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryQualityTest {

    private val entries = TestDictionary.entries
    private val trie = TestDictionary.trie

    @Test
    fun junkIsAbsent() {
        for (word in listOf("tj", "gp", "hte", "teh", "im", "dont", "cs", "dog's")) {
            assertFalse(word, word in trie)
        }
    }

    @Test
    fun commonWordsArePresent() {
        val expected = mapOf(
            "the" to "the",
            "i" to "I",
            "don't" to "don't",
            "you're" to "you're",
            "it's" to "it's",
            "monday" to "Monday",
            "english" to "English",
            "okay" to "okay",
            "gonna" to "gonna",
            "tv" to "TV",
            "iphone" to "iPhone",
        )
        for ((key, surface) in expected) {
            val node = trie.find(key)
            assertTrue(key, node != ArrayTrie.NO_NODE && trie.isTerminal(node))
            assertEquals(surface, trie.primarySurface(node, key))
        }
    }

    @Test
    fun accentedSpellingsAreKept() {
        val surfaces = ArrayList<String>()
        trie.forEachWord(trie.find("cafe"), "cafe") { surface, _, _ -> surfaces.add(surface) }
        assertTrue(surfaces.toString(), "café" in surfaces)
    }

    @Test
    fun offensiveWordsAreFlagged() {
        assertEquals(ArrayTrie.OFFENSIVE, trie.wordFlags(trie.find("shit")) and ArrayTrie.OFFENSIVE)
    }

    @Test
    fun sizeIsReasonable() {
        assertTrue("${entries.size} entries", entries.size in 50_000..90_000)
    }

    @Test
    fun entriesAreSortedUniqueAndInAlphabet() {
        for (i in entries.indices) {
            val entry = entries[i]
            assertEquals(entry.key, Alphabet.fold(entry.key))
            assertEquals(entry.key, Alphabet.fold(entry.surface ?: entry.key))
            assertTrue(entry.key, entry.freq in 1..ArrayTrie.MAX_FREQ)
            if (i > 0) {
                val previous = entries[i - 1]
                assertTrue(entry.key, previous.key <= entry.key)
                assertFalse(entry.key, previous.key == entry.key && previous.surface == entry.surface)
            }
        }
    }

    @Test
    fun frequenciesAreSensible() {
        val the = trie.ownFreq(trie.find("the"))
        assertEquals(the, trie.maxFreq(ArrayTrie.ROOT))
        assertTrue(trie.ownFreq(trie.find("receive")) > trie.ownFreq(trie.find("recite")))
    }
}
