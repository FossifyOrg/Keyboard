package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentWordTest {

    private fun find(before: String, after: String = "") = CurrentWord.find(before, after)

    @Test
    fun wordBeforeCursorIsFound() {
        assertEquals("teh", find("I saw teh")?.word)
        assertEquals("teh", find("teh")?.word)
        assertEquals("don't", find("I don't")?.word)
        assertEquals("don’t", find("I don’t")?.word)
        assertEquals("well-", find("a well-")?.word)
        assertEquals("café", find("the café")?.word)
    }

    @Test
    fun tokenHoldsWhatComesBeforeTheWord() {
        assertEquals("@teh", find("hi @teh")?.token)
        assertEquals("example.com/teh", find("see example.com/teh")?.token)
        assertEquals("teh", find("example.com/teh")?.word)
        assertEquals("(teh", find("a (teh")?.token)
    }

    @Test
    fun quotesAndDashesBeforeTheWordAreLeftOut() {
        assertEquals("teh", find("'teh")?.word)
        assertEquals("teh", find("-teh")?.word)
        assertEquals("'teh", find("'teh")?.token)
    }

    @Test
    fun noWordIsFoundOutsideWords() {
        assertNull(find(""))
        assertNull(find("teh "))
        assertNull(find("teh."))
        assertNull(find("b4"))
        assertNull(find("'"))
        assertNull(find("a".repeat(CurrentWord.MAX_LENGTH + 1)))
    }

    @Test
    fun noWordIsFoundInsideAWord() {
        assertNull(find("te", "h"))
        assertNull(find("don", "'t"))
        assertEquals("teh", find("teh", " there")?.word)
        assertEquals("teh", find("teh", ".")?.word)
    }
}
