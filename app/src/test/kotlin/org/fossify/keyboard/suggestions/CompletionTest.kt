package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionTest {

    private val trie = TestDictionary.trie
    private val search = FuzzySearch(trie)

    @Test
    fun completionsAreOrderedByFrequency() {
        val completions = search.completions(trie.find("th"), "th", 3)
        assertEquals(listOf("the", "that", "this"), completions.map { it.key })
    }

    @Test
    fun completionsDontIncludeThePrefixItself() {
        val completions = search.completions(trie.find("the"), "the", 5).map { it.key }
        assertTrue("the" !in completions)
        assertTrue(completions.all { it.startsWith("the") })
    }

    @Test
    fun offensiveWordsAreNotCompleted() {
        val completions = search.completions(trie.find("sh"), "sh", 50).map { it.key }
        assertTrue("shit" !in completions)
        assertTrue("she" in completions)
    }

    @Test
    fun offensiveWordsAreNotSuggested() {
        val engine = SuggestionEngine(trie)
        assertTrue(engine.isWord("shit"))
        assertTrue("shit" !in engine.suggest("shit").candidates.map { it.word })
        assertTrue("shit" !in engine.suggest("shiy").candidates.map { it.word })
    }

    @Test
    fun enginePrefersCompletionsOfTheTypedPrefix() {
        val engine = SuggestionEngine(trie)
        assertEquals(listOf("the", "that", "this"), engine.suggest("th").words)
        assertEquals("because", engine.suggest("becau").words.first())
    }
}
