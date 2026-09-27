package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutocorrectPolicyTest {

    private val engine = SuggestionEngine(TestDictionary.trie)

    private fun correct(
        token: String,
        typed: String = token,
        reverted: Set<String> = emptySet(),
        isStickyUserWord: Boolean = false,
    ) = AutocorrectPolicy.correctionFor(token, engine.suggest(typed), reverted, isStickyUserWord)?.word

    private fun correct(suggestions: Suggestions) =
        AutocorrectPolicy.correctionFor("wrd", suggestions, emptySet(), isStickyUserWord = false)?.word

    @Test
    fun typosAreCorrected() {
        assertEquals("the", correct("teh"))
        assertEquals("The", correct("Teh"))
        assertEquals("receive", correct("recieve"))
        assertEquals("because", correct("becuase"))
    }

    @Test
    fun caseAndApostrophesAreFixed() {
        assertEquals("I", correct("i"))
        assertEquals("Monday", correct("monday"))
        assertEquals("don't", correct("dont"))
        assertEquals("I'm", correct("im"))
    }

    @Test
    fun validWordsNeverChange() {
        for (word in listOf("ill", "its", "well", "were", "cant", "wont", "hell", "cafe", "the", "I", "Monday")) {
            assertNull(word, correct(word))
        }
    }

    @Test
    fun specialTokensAreSkipped() {
        assertNull(correct("b4"))
        assertNull(correct("teh2"))
        assertNull(correct("@teh", "teh"))
        assertNull(correct("#teh", "teh"))
        assertNull(correct("example.com/teh", "teh"))
        assertNull(correct("https://teh", "teh"))
        assertNull(correct("teh_teh", "teh"))
        assertNull(correct("me@teh", "teh"))
        assertNull(correct("TEH"))
        assertNull(correct("NASA"))
        assertNull(correct("a"))
        assertFalse(AutocorrectPolicy.isCorrectable("x", "x"))
        assertTrue(AutocorrectPolicy.isCorrectable("i", "i"))
        assertTrue(AutocorrectPolicy.isCorrectable("(teh", "teh"))
    }

    @Test
    fun revertedAndLearnedWordsAreKept() {
        assertNull(correct("teh", reverted = setOf("teh")))
        assertNull(correct("teh", isStickyUserWord = true))
    }

    @Test
    fun closeCallsAreLeftAlone() {
        val suggestions = { runnerUpScore: Float, unknownWordScore: Float ->
            Suggestions(
                typed = "wrd",
                candidates = listOf(
                    ScoredWord("word", 10f, 0.5f, 0, isExact = false, isCompletion = false),
                    ScoredWord("ward", runnerUpScore, 0.5f, 0, isExact = false, isCompletion = false),
                ),
                typedIsWord = false,
                unknownWordScore = unknownWordScore,
                maxCost = 1.5f,
            )
        }

        val margin = EngineConstants.AUTOCORRECT_MARGIN
        assertEquals("word", correct(suggestions(10f - margin, 5f)))
        assertNull(correct(suggestions(10f - margin / 2, 5f)))
        assertNull(correct(suggestions(5f, 10f)))
    }

    @Test
    fun unsafeCandidatesAreNeverApplied() {
        val candidate = { flags: Int, isCompletion: Boolean, cost: Float ->
            Suggestions(
                typed = "wrd",
                candidates = listOf(ScoredWord("word", 10f, cost, flags, isExact = false, isCompletion = isCompletion)),
                typedIsWord = false,
                unknownWordScore = 0f,
                maxCost = 1.5f,
            )
        }

        assertEquals("word", correct(candidate(0, false, 1f)))
        assertNull(correct(candidate(ArrayTrie.OFFENSIVE, false, 1f)))
        assertNull(correct(candidate(ArrayTrie.NO_AUTOCORRECT_TO, false, 1f)))
        assertNull(correct(candidate(0, true, 1f)))
        assertNull(correct(candidate(0, false, 2f)))
    }

    @Test
    fun learnedWordsThatArentStickyAreCorrectedLikeUnknownWords() {
        val suggestions = Suggestions(
            typed = "wuick",
            candidates = listOf(
                ScoredWord("wuick", 11f, 0f, 0, isExact = true, isCompletion = false),
                ScoredWord("quick", 10f, 0.8f, 0, isExact = false, isCompletion = false),
                ScoredWord("wick", 8f, 0.8f, 0, isExact = false, isCompletion = false),
            ),
            typedIsWord = false,
            unknownWordScore = 7f,
            maxCost = 2f,
        )

        assertEquals("quick", AutocorrectPolicy.correctionFor("wuick", suggestions, emptySet(), false)?.word)
        assertNull(AutocorrectPolicy.correctionFor("wuick", suggestions, emptySet(), isStickyUserWord = true))
    }
}
