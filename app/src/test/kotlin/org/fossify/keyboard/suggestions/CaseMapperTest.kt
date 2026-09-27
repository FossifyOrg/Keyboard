package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Test

class CaseMapperTest {

    @Test
    fun patterns() {
        assertEquals(CaseMapper.LOWER, CaseMapper.patternOf("hello"))
        assertEquals(CaseMapper.LOWER, CaseMapper.patternOf("don't"))
        assertEquals(CaseMapper.TITLE, CaseMapper.patternOf("Hello"))
        assertEquals(CaseMapper.TITLE, CaseMapper.patternOf("I"))
        assertEquals(CaseMapper.UPPER, CaseMapper.patternOf("HELLO"))
        assertEquals(CaseMapper.UPPER, CaseMapper.patternOf("DON'T"))
        assertEquals(CaseMapper.MIXED, CaseMapper.patternOf("hELLO"))
        assertEquals(CaseMapper.MIXED, CaseMapper.patternOf("McDonald"))
    }

    @Test
    fun lowercaseKeepsDictionaryCase() {
        assertEquals("I", CaseMapper.apply("I", CaseMapper.LOWER))
        assertEquals("Monday", CaseMapper.apply("Monday", CaseMapper.LOWER))
        assertEquals("iPhone", CaseMapper.apply("iPhone", CaseMapper.LOWER))
        assertEquals("the", CaseMapper.apply("the", CaseMapper.LOWER))
    }

    @Test
    fun titleCapitalizesTheFirstLetter() {
        assertEquals("The", CaseMapper.apply("the", CaseMapper.TITLE))
        assertEquals("Don't", CaseMapper.apply("don't", CaseMapper.TITLE))
        assertEquals("Monday", CaseMapper.apply("Monday", CaseMapper.TITLE))
        assertEquals("iPhone", CaseMapper.apply("iPhone", CaseMapper.TITLE))
    }

    @Test
    fun allCapsUppercases() {
        assertEquals("THE", CaseMapper.apply("the", CaseMapper.UPPER))
        assertEquals("IPHONE", CaseMapper.apply("iPhone", CaseMapper.UPPER))
        assertEquals("CAFÉ", CaseMapper.apply("café", CaseMapper.UPPER))
    }
}
