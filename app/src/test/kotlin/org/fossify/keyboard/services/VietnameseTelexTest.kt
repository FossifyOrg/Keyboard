package org.fossify.keyboard.services

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VietnameseTelexTest {

    private lateinit var telexRules: HashMap<String, String>

    @Before
    fun setup() {
        telexRules = hashMapOf(
            "w" to "ư",
            "ww" to "ư",
            "a" to "ă",
            "aw" to "ă",
            "aa" to "â",
            "dd" to "đ",
            "ee" to "ê",
            "oo" to "ô",
            "ow" to "ơ",
            "uw" to "ư",
            "uow" to "ươ"
        )
    }

    @Test
    fun testShortestMatchFirst() {
        val input = "aw"
        val result = applyRulesShortestFirst(input)
        assertEquals("aư", result)
    }

    @Test
    fun testLongestMatchFirst() {
        val input = "aw"
        val result = applyRulesLongestFirst(input)
        assertEquals("ă", result)
    }

    @Test
    fun testDoubleA() {
        val input = "aa"
        val result = applyRulesLongestFirst(input)
        assertEquals("â", result)
    }

    @Test
    fun testTripleCharacter() {
        val input = "uow"
        val result = applyRulesLongestFirst(input)
        assertEquals("ươ", result)
    }

    @Test
    fun testNoTransformation() {
        val input = "b"
        val result = applyRulesLongestFirst(input)
        assertEquals("b", result)
    }

    @Test
    fun testDoubleWTransformation() {
        val input = "ww"
        val shortestFirstResult = applyRulesShortestFirst(input)
        assertEquals("wư", shortestFirstResult)
        
        val longestFirstResult = applyRulesLongestFirst(input)
        assertEquals("ư", longestFirstResult)
    }

    @Test
    fun testDoubleDTransformation() {
        val input = "dd"
        val result = applyRulesLongestFirst(input)
        assertEquals("đ", result)
    }

    @Test
    fun testMixedCaseNotMatching() {
        val input = "Ee"
        val result = applyRulesCaseSensitive(input)
        assertEquals("Ee", result)
    }

    @Test
    fun testAllUppercaseNotMatching() {
        val input = "EE"
        val result = applyRulesCaseSensitive(input)
        assertEquals("EE", result)
    }

    @Test
    fun testPatternPrecedence() {
        val input = "aw"
        
        val shortestFirstResult = applyRulesShortestFirst(input)
        assertEquals("aư", shortestFirstResult)
        
        val longestFirstResult = applyRulesLongestFirst(input)
        assertEquals("ă", longestFirstResult)
    }

    @Test
    fun testSingleW() {
        val input = "w"
        val result = applyRulesLongestFirst(input)
        assertEquals("ư", result)
    }

    @Test
    fun testTripleW() {
        val input = "www"
        val result = applyRulesLongestFirst(input)
        assertEquals("wư", result)
    }

    @Test
    fun testSpaceDoubleW() {
        val input = "O ww"
        val result = applyRulesLongestFirst(input)
        assertEquals("O ư", result)
    }

    @Test
    fun testUppercaseCasePreservation_Aw() {
        val input = "Aw"
        val result = applyRulesWithCasePreservation(input)
        assertEquals("Ă", result)
    }

    @Test
    fun testLowercaseCasePreservation_aw() {
        val input = "aw"
        val result = applyRulesWithCasePreservation(input)
        assertEquals("ă", result)
    }

    @Test
    fun testUppercaseCasePreservation_Ow() {
        val input = "Ow"
        val result = applyRulesWithCasePreservation(input)
        assertEquals("Ơ", result)
    }

    @Test
    fun testLowercaseCasePreservation_ow() {
        val input = "ow"
        val result = applyRulesWithCasePreservation(input)
        assertEquals("ơ", result)
    }

    /**
     * Helper function that applies transformation rules checking shortest patterns first.
     * This demonstrates incorrect behavior when shorter patterns match before longer ones.
     */
    private fun applyRulesShortestFirst(word: String): String {
        val wordChars = word.toCharArray()
        val predictWord = StringBuilder()
        
        for (char in wordChars.size - 1 downTo 0) {
            predictWord.append(wordChars[char])
            val shouldChangeText = predictWord.reverse().toString()
            val shouldChangeTextLower = shouldChangeText.lowercase()
            
            if (telexRules.containsKey(shouldChangeTextLower)) {
                val prefix = word.substring(0, word.length - shouldChangeText.length)
                return prefix + telexRules[shouldChangeTextLower]
            }
            
            predictWord.reverse()
        }
        
        return word
    }

    /**
     * Helper function that applies transformation rules checking longest patterns first.
     * This demonstrates correct behavior where longer patterns take precedence.
     */
    private fun applyRulesLongestFirst(word: String): String {
        for (length in word.length downTo 1) {
            val suffix = word.substring(word.length - length)
            val suffixLower = suffix.lowercase()
            if (telexRules.containsKey(suffixLower)) {
                val prefix = word.substring(0, word.length - length)
                return prefix + telexRules[suffixLower]!!
            }
        }
        
        return word
    }

    /**
     * Helper function that applies transformation rules with case preservation.
     * Matches case-insensitively but preserves the original case in output.
     */
    private fun applyRulesWithCasePreservation(word: String): String {
        for (length in word.length downTo 1) {
            val suffix = word.substring(word.length - length)
            val suffixLower = suffix.lowercase()
            if (telexRules.containsKey(suffixLower)) {
                val prefix = word.substring(0, word.length - length)
                val replacement = telexRules[suffixLower]!!
                // Preserve case: if first char is uppercase, capitalize replacement
                val finalReplacement = if (suffix.firstOrNull()?.isUpperCase() == true && replacement.isNotEmpty()) {
                    replacement.replaceFirstChar { it.uppercase() }
                } else {
                    replacement
                }
                return prefix + finalReplacement
            }
        }
        return word
    }

    /**
     * Helper function that applies rules with case-sensitive matching.
     * Mixed-case input won't match lowercase rules.
     */
    private fun applyRulesCaseSensitive(word: String): String {
        for (length in word.length downTo 1) {
            val suffix = word.substring(word.length - length)
            if (telexRules.containsKey(suffix)) {
                val prefix = word.substring(0, word.length - length)
                return prefix + telexRules[suffix]!!
            }
        }
        
        return word
    }

}
