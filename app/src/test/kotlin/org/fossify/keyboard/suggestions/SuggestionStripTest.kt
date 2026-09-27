package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionStripTest {

    private fun suggestions(vararg words: String) = Suggestions(
        typed = "typed",
        candidates = words.mapIndexed { i, word ->
            ScoredWord(word, score = -i.toFloat(), cost = 0f, flags = 0, isExact = false, isCompletion = false)
        },
        typedIsWord = false,
    )

    @Test
    fun bestWordGoesInTheMiddle() {
        val chips = SuggestionStrip.arrange(suggestions("the", "they", "then", "there"))
        assertEquals(listOf("they", "the", "then"), chips.map { it?.word })
    }

    @Test
    fun missingSuggestionsLeaveEmptyChips() {
        val chips = SuggestionStrip.arrange(suggestions("the"))
        assertEquals(SuggestionStrip.SIZE, chips.size)
        assertEquals("the", chips[SuggestionStrip.MIDDLE]?.word)
        assertNull(chips[SuggestionStrip.LEFT])
        assertNull(chips[SuggestionStrip.RIGHT])
    }

    @Test
    fun pendingCorrectionGoesInTheMiddleWithTheTypedWordOnTheLeft() {
        val suggestions = suggestions("the", "teh", "ten", "they")
        val chips = SuggestionStrip.arrange(suggestions, suggestions.candidates[0])
        assertEquals(SuggestionChip("typed", isTyped = true), chips[SuggestionStrip.LEFT])
        assertEquals(SuggestionChip("the", isCorrection = true), chips[SuggestionStrip.MIDDLE])
        assertEquals(SuggestionChip("teh"), chips[SuggestionStrip.RIGHT])
    }

    @Test
    fun noSuggestionsShowNothing() {
        assertTrue(SuggestionStrip.arrange(suggestions()).isEmpty())
    }
}
