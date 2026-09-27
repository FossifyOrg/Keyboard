package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JaroWinklerTest {

    @Test
    fun knownValues() {
        assertEquals(0.961f, JaroWinkler.similarity("martha", "marhta"), 0.001f)
        assertEquals(0.840f, JaroWinkler.similarity("dwayne", "duane"), 0.001f)
        assertEquals(0.813f, JaroWinkler.similarity("dixon", "dicksonx"), 0.001f)
        assertEquals(1f, JaroWinkler.similarity("same", "same"), 0f)
        assertEquals(0f, JaroWinkler.similarity("abc", "xyz"), 0f)
        assertEquals(0f, JaroWinkler.similarity("", "abc"), 0f)
    }

    @Test
    fun sharedPrefixScoresHigher() {
        assertTrue(JaroWinkler.similarity("thw", "the") > JaroWinkler.similarity("thw", "why"))
    }
}
