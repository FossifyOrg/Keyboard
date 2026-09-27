package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserLexiconTest {

    private val model = KeyboardErrorModel(KeyboardGeometry.QWERTY)

    private fun symbols(word: String) = IntArray(word.length) { Alphabet.symbolOf(Alphabet.fold(word[it])) }

    private fun UserLexicon.candidatesFor(typed: String) =
        candidates(symbols(typed), model, model.maxCost(typed.length)).associateBy { it.word }

    @Test
    fun learnedWordsAreCountedByFoldedKey() {
        val lexicon = UserLexicon()
        assertEquals(1, lexicon.learn("Kayleigh")?.count)
        assertEquals(2, lexicon.learn("KAYLEIGH")?.count)
        assertTrue("kayleigh" in lexicon)
        assertTrue("Kayleigh" in lexicon)
        assertFalse("Kaylee" in lexicon)
        assertNull(lexicon.learn("b4"))
        assertEquals(1, lexicon.size)
    }

    @Test
    fun confirmationsAreCounted() {
        val lexicon = UserLexicon()
        assertEquals(0, lexicon.uses("yeet"))
        assertEquals(2, lexicon.learn("yeet", uses = 2)?.count)
        lexicon.learn("YEET")
        assertEquals(3, lexicon.uses("yeet"))
        assertEquals(0, lexicon.uses("b4"))
    }

    @Test
    fun lowercaseUseReplacesACapital() {
        val lexicon = UserLexicon()
        lexicon.learn("Yeet")
        val learned = lexicon.learn("yeet")!!
        assertEquals("yeet", learned.word)
        assertEquals("Yeet", learned.replaced)
        assertEquals("yeet", lexicon.learn("Yeet")!!.word)
    }

    @Test
    fun leastRecentlyUsedWordsMakeRoom() {
        val lexicon = UserLexicon(capacity = 2)
        lexicon.learn("alpha")
        lexicon.learn("bravo")
        lexicon.learn("alpha")
        lexicon.learn("charlie")
        assertTrue("alpha" in lexicon)
        assertFalse("bravo" in lexicon)
        assertTrue("charlie" in lexicon)
    }

    @Test
    fun loadedWordsAddUpWithNewUses() {
        val lexicon = UserLexicon()
        lexicon.learn("yeet")
        lexicon.add("yeet", 3)
        assertEquals(5, lexicon.learn("yeet")?.count)
    }

    @Test
    fun changesUpdateTheVersion() {
        val lexicon = UserLexicon()
        val versions = mutableListOf(lexicon.version)
        lexicon.learn("yeet")
        versions.add(lexicon.version)
        lexicon.add("rizz", 1)
        versions.add(lexicon.version)
        lexicon.clear()
        versions.add(lexicon.version)
        assertEquals(versions.size, versions.toSet().size)
        assertEquals(0, lexicon.size)
    }

    @Test
    fun learnedWordsAreCorrectedAndCompleted() {
        val lexicon = UserLexicon().apply {
            learn("Kayleigh")
            learn("yeet")
        }

        assertEquals(0f, lexicon.candidatesFor("kayleigh").getValue("Kayleigh").cost)
        assertFalse(lexicon.candidatesFor("kayliegh").getValue("Kayleigh").isCompletion)
        assertTrue(lexicon.candidatesFor("kayl").getValue("Kayleigh").isCompletion)
        assertTrue(lexicon.candidatesFor("kaul").getValue("Kayleigh").isCompletion)
        assertTrue(lexicon.candidatesFor("ye").getValue("yeet").isCompletion)
        assertFalse("yeet" in lexicon.candidatesFor("yr"))
        assertFalse("Kayleigh" in lexicon.candidatesFor("house"))
    }

    @Test
    fun moreUsesRankHigher() {
        val lexicon = UserLexicon()
        lexicon.learn("yeet")
        val once = lexicon.candidatesFor("yeet").getValue("yeet").freq
        repeat(5) { lexicon.learn("yeet") }
        assertNotEquals(once, lexicon.candidatesFor("yeet").getValue("yeet").freq)
        assertTrue(lexicon.candidatesFor("yeet").getValue("yeet").freq > once)
    }

    @Test
    fun alignmentCostsMatchTheTrieSearch() {
        val trie = TestDictionary.trie
        val search = FuzzySearch(trie)
        for (typed in listOf("teh", "recieve", "adress", "wrod", "helo", "dont", "yhe", "becuase", "thier")) {
            val typedSymbols = symbols(typed)
            val maxCost = model.maxCost(typed.length)
            val aligner = Aligner(model, typedSymbols, maxCost)
            for (candidate in search.corrections(typedSymbols, model, maxCost, withCompletions = false)) {
                val cost = aligner.align(symbols(candidate.key)).cost
                assertEquals("$typed -> ${candidate.key}", candidate.cost, cost, 1e-4f)
            }
        }
    }

    @Test
    fun aFullLexiconKeepsSuggestionsFast() {
        // Learned words that look like words: dictionary words with their last letter changed
        val learned = TestDictionary.entries.asSequence()
            .map { it.key }
            .filter { it.length >= MIN_LEARNED_LENGTH && it.all { c -> c in 'a'..'z' } }
            .map { it.dropLast(1) + if (it.last() == 'q') 'z' else 'q' }
            .filter { it !in TestDictionary.trie }
            .distinct()
            .take(EngineConstants.MAX_USER_WORDS)
            .toList()

        val queries = Evaluation.syntheticCases(LATENCY_QUERIES).map { it.typed }
        val engine = SuggestionEngine(TestDictionary.trie)
        val (_, withoutWords) = Evaluation.latency({ engine.suggest(it).words }, queries)
        engine.userLexicon = UserLexicon().apply { learned.forEach { learn(it) } }
        val (_, withWords) = Evaluation.latency({ engine.suggest(it).words }, queries)

        println("p95 µs without learned words: %.0f, with %d: %.0f".format(withoutWords, learned.size, withWords))
        assertEquals(EngineConstants.MAX_USER_WORDS, learned.size)
        assertTrue(withWords < MAX_P95_MICROS)
    }

    private companion object {
        const val MIN_LEARNED_LENGTH = 4
        const val LATENCY_QUERIES = 1000
        const val MAX_P95_MICROS = 10_000f
    }
}
