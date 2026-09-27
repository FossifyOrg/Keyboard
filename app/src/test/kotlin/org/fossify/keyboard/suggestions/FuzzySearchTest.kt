package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchTest {

    private val engine = SuggestionEngine(TestDictionary.trie)

    private fun best(typed: String) = engine.suggest(typed).words.first()

    private fun top3(typed: String) = engine.suggest(typed).words

    @Test
    fun commonTyposAreCorrected() {
        val expected = mapOf(
            "teh" to "the",
            "yhe" to "the",
            "recieve" to "receive",
            "wrod" to "word",
            "adress" to "address",
            "thier" to "their",
            "becuase" to "because",
            "tomorow" to "tomorrow",
            "definately" to "definitely",
            "goign" to "going",
            "knwo" to "know",
        )
        for ((typed, word) in expected) {
            assertEquals(typed, word, best(typed))
        }
    }

    @Test
    fun missingDoubleLettersAreSuggested() {
        assertEquals("happy", best("hapy"))
        assertEquals("tomorrow", best("tomorow"))
        assertTrue("hello" in top3("helo"))
    }

    @Test
    fun apostrophesAreRestored() {
        assertEquals("don't", best("dont"))
        assertEquals("I'm", best("im"))
        assertTrue("it's" in top3("its"))
    }

    @Test
    fun dictionaryCaseIsApplied() {
        assertEquals("I", best("i"))
        assertEquals("Monday", best("monday"))
        assertEquals("café", best("cafe"))
    }

    @Test
    fun typedCaseIsKept() {
        assertEquals("The", best("Teh"))
        assertEquals("THE", best("TEH"))
    }

    @Test
    fun validWordsComeFirst() {
        for (word in listOf("ill", "its", "well", "were", "form")) {
            val suggestions = engine.suggest(word)
            assertTrue(word, suggestions.typedIsWord)
            assertEquals(word, suggestions.words.first())
        }
    }

    @Test
    fun wordsWithDigitsHaveNoSuggestions() {
        assertTrue(engine.suggest("b4").candidates.isEmpty())
        assertTrue(engine.suggest("").candidates.isEmpty())
    }

    @Test
    fun neighbouringKeysAreCheaper() {
        val qwerty = KeyboardErrorModel(KeyboardGeometry.QWERTY)
        val q = Alphabet.symbolOf('q')
        val p = Alphabet.symbolOf('p')
        assertTrue(qwerty.substitutionCost(q, Alphabet.symbolOf('w')) < qwerty.substitutionCost(q, p))
        assertEquals(EngineConstants.SUB_MAX, qwerty.substitutionCost(q, Alphabet.symbolOf('p')))
        assertTrue(KeyboardGeometry.QWERTY.areNeighbours(q, Alphabet.symbolOf('a')))
    }

    @Test
    fun layoutChangesCosts() {
        val dvorak = KeyboardGeometry.fromRows(
            listOf("',.pyfgcrl", "aoeuidhtns", ";qjkxbmwvz"),
            listOf(0f, 0.5f, 1.5f)
        )
        val qwerty = KeyboardErrorModel(KeyboardGeometry.QWERTY)
        val dvorakCosts = KeyboardErrorModel(dvorak)
        val t = Alphabet.symbolOf('t')
        val h = Alphabet.symbolOf('h')
        val y = Alphabet.symbolOf('y')
        assertTrue(dvorakCosts.substitutionCost(t, h) < qwerty.substitutionCost(t, h))
        assertTrue(dvorakCosts.substitutionCost(t, y) > qwerty.substitutionCost(t, y))

        // D is next to H on Dvorak but not on QWERTY
        val dvorakEngine = SuggestionEngine(TestDictionary.trie, dvorak)
        assertEquals("the", dvorakEngine.suggest("tde").words.first())
        val qwertyCost = engine.suggest("tde").candidates.firstOrNull { it.word == "the" }?.cost ?: Float.MAX_VALUE
        val dvorakCost = dvorakEngine.suggest("tde").candidates.first { it.word == "the" }.cost
        assertTrue(dvorakCost < qwertyCost)
    }

    @Test
    fun transpositionsAreCheaperThanTwoSubstitutions() {
        val candidate = engine.suggest("hte").candidates.first { it.word == "the" }
        assertEquals(EngineConstants.TRANSPOSITION * EngineConstants.FIRST_LETTER_FACTOR, candidate.cost, 0.3f)
        assertTrue(candidate.cost < 1f)
    }
}
