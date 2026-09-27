package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionSessionTest {

    private val session = SuggestionSession().apply {
        engine = SuggestionEngine(TestDictionary.trie)
        isEnabled = true
    }

    private val words get() = session.chips.map { it?.word }

    @Test
    fun suggestionsFollowTheWordBeforeTheCursor() {
        val field = FakeTextField("I saw teh|")
        assertTrue(session.refresh(field))
        assertEquals("the", words[SuggestionStrip.MIDDLE])

        assertFalse(session.refresh(field))

        field.type(" ")
        assertTrue(session.refresh(field))
        assertTrue(session.chips.isEmpty())
    }

    @Test
    fun noSuggestionsInsideAWord() {
        session.refresh(FakeTextField("te|h"))
        assertTrue(session.chips.isEmpty())
    }

    @Test
    fun noSuggestionsWhenDisabledOrWithoutDictionary() {
        val field = FakeTextField("teh|")
        session.isEnabled = false
        session.refresh(field)
        assertTrue(session.chips.isEmpty())

        session.isEnabled = true
        session.engine = null
        session.refresh(field)
        assertTrue(session.chips.isEmpty())
    }

    @Test
    fun pickingReplacesTheWordAndAddsASpace() {
        val field = FakeTextField("I saw teh|")
        session.refresh(field)
        assertTrue(session.pick(field, SuggestionStrip.MIDDLE))
        assertEquals("I saw the |", field.toString())
        assertEquals(1, field.edits)
        assertTrue(session.chips.isEmpty())
    }

    @Test
    fun pickingKeepsTheTextAfterTheCursor() {
        val field = FakeTextField("Teh| end")
        session.refresh(field)
        assertTrue(session.pick(field, SuggestionStrip.MIDDLE))
        assertEquals("The | end", field.toString())
    }

    @Test
    fun staleOrEmptyChipsAreNotPicked() {
        val field = FakeTextField("teh|")
        session.refresh(field)
        field.type("x")
        assertFalse(session.pick(field, SuggestionStrip.MIDDLE))
        assertEquals("tehx|", field.toString())

        session.refresh(FakeTextField("|"))
        assertFalse(session.pick(field, SuggestionStrip.MIDDLE))
        assertEquals(0, field.edits)
    }

    /** Types [chars] into [field] the way the keyboard does, with autocorrect before separators. */
    private fun type(field: FakeTextField, chars: String) {
        for (char in chars) {
            if (!session.onCharTyped(field, char) && char != '\n') field.type(char.toString())
            if (char == '\n') field.type("\n")
            session.refresh(field)
        }
    }

    private fun autocorrecting() = session.apply { isAutocorrectEnabled = true }

    @Test
    fun separatorsCorrectTheWordBeforeThem() {
        autocorrecting()
        for (separator in " .,!?;:)") {
            val field = FakeTextField("")
            type(field, "I saw teh$separator")
            assertEquals("I saw the$separator|", field.toString())
        }
    }

    @Test
    fun caseAndApostrophesAreFixed() {
        autocorrecting()
        val field = FakeTextField("")
        type(field, "i think im late on monday ")
        assertEquals("I think I'm late on Monday |", field.toString())
    }

    @Test
    fun newLineCorrectsWithoutTypingIt() {
        autocorrecting()
        val field = FakeTextField("teh|")
        assertFalse(session.onCharTyped(field, '\n'))
        assertEquals("the|", field.toString())
    }

    @Test
    fun nothingIsCorrectedWithoutAutocorrect() {
        val field = FakeTextField("")
        type(field, "teh ")
        assertEquals("teh |", field.toString())
    }

    @Test
    fun lettersAndValidWordsAreNotCorrected() {
        autocorrecting()
        val field = FakeTextField("")
        type(field, "tehx well b4 ")
        assertEquals("tehx well b4 |", field.toString())
    }

    @Test
    fun singleLettersBeforeAPeriodAreKept() {
        autocorrecting()
        val field = FakeTextField("")
        type(field, "i.e. i ")
        assertEquals("i.e. I |", field.toString())
    }

    @Test
    fun pendingCorrectionIsShownOnTheStrip() {
        autocorrecting()
        session.refresh(FakeTextField("teh|"))
        assertEquals(SuggestionChip("teh", isTyped = true), session.chips[SuggestionStrip.LEFT])
        assertEquals(SuggestionChip("the", isCorrection = true), session.chips[SuggestionStrip.MIDDLE])
    }

    @Test
    fun backspaceRightAfterACorrectionUndoesIt() {
        autocorrecting()
        val field = FakeTextField("| end")
        type(field, "I saw teh ")
        assertEquals("I saw the | end", field.toString())
        assertTrue(session.undoCorrection(field))
        assertEquals("I saw teh| end", field.toString())

        // The word stays as typed and isn't corrected again
        assertFalse(session.undoCorrection(field))
        type(field, " ")
        assertEquals("I saw teh | end", field.toString())
    }

    @Test
    fun newLineCorrectionsCanBeUndone() {
        autocorrecting()
        val field = FakeTextField("")
        type(field, "teh\n")
        assertEquals("the\n|", field.toString())
        assertTrue(session.undoCorrection(field))
        assertEquals("teh|", field.toString())
    }

    @Test
    fun typingOrMovingTheCursorKeepsTheCorrection() {
        autocorrecting()
        val field = FakeTextField("")
        type(field, "teh x")
        field.replaceBeforeCursor(1, "")
        session.refresh(field)
        assertFalse(session.undoCorrection(field))
        assertEquals("the |", field.toString())

        type(field, "teh ")
        field.cursor = 0
        session.refresh(field)
        field.cursor = field.toString().length - 1
        assertFalse(session.undoCorrection(field))
        assertEquals("the the |", field.toString())
    }

    @Test
    fun pickingTheTypedWordKeepsIt() {
        autocorrecting()
        val field = FakeTextField("teh|")
        session.refresh(field)
        assertTrue(session.pick(field, SuggestionStrip.LEFT))
        assertEquals("teh |", field.toString())

        type(field, "teh ")
        assertEquals("teh teh |", field.toString())
    }

    @Test
    fun revertedWordsAreForgottenInTheNextField() {
        autocorrecting()
        val field = FakeTextField("")
        type(field, "teh ")
        session.undoCorrection(field)
        type(field, " ")
        session.reset()
        type(field, "teh ")
        assertEquals("teh the |", field.toString())
    }

    @Test
    fun revertedWordsAreForgottenWithAnotherDictionary() {
        autocorrecting()
        val field = FakeTextField("")
        type(field, "teh ")
        session.undoCorrection(field)
        type(field, " ")
        session.engine = SuggestionEngine(TestDictionary.trie)
        type(field, "teh ")
        assertEquals("teh the |", field.toString())
    }

    private fun learning(): UserLexicon {
        val lexicon = UserLexicon()
        session.engine!!.userLexicon = lexicon
        session.isLearningEnabled = true
        session.onLearn = { word, uses -> lexicon.learn(word, uses) }
        return lexicon
    }

    @Test
    fun anAccidentalUndoIsForgotten() {
        autocorrecting()
        val lexicon = learning()
        val field = FakeTextField("")
        type(field, "The wuick ")
        assertEquals("The quick |", field.toString())
        session.undoCorrection(field)
        assertEquals("The wuick|", field.toString())
        assertFalse("wuick" in lexicon)

        // The user fixes the word instead of keeping it
        field.replaceBeforeCursor("wuick".length, "")
        type(field, "quick ")
        session.reset()
        type(field, "wuick ")
        assertEquals("The quick quick |", field.toString())
        assertFalse("wuick" in lexicon)
    }

    @Test
    fun undoneTyposOnlyStickAfterRepeatedConfirmations() {
        autocorrecting()
        val lexicon = learning()
        val field = FakeTextField("")

        // Undoing and keeping it learns it, but it's still corrected in the next text field
        type(field, "wuick ")
        session.undoCorrection(field)
        type(field, " ")
        assertEquals(EngineConstants.LEARN_AFTER_USES, lexicon.uses("wuick"))
        session.reset()
        type(field, "wuick ")
        assertEquals("wuick quick |", field.toString())

        // Undoing it again makes it stick
        session.undoCorrection(field)
        type(field, " ")
        assertTrue(lexicon.uses("wuick") >= EngineConstants.STICKY_USES)
        session.reset()
        type(field, "wuick ")
        assertEquals("wuick wuick wuick |", field.toString())
    }

    @Test
    fun keptTypedWordsAreLearnedButNotStickyYet() {
        autocorrecting()
        val lexicon = learning()
        val field = FakeTextField("teh|")
        session.refresh(field)
        session.pick(field, SuggestionStrip.LEFT)
        assertEquals(EngineConstants.LEARN_AFTER_USES, lexicon.uses("teh"))

        session.reset()
        type(field, "teh ")
        assertEquals("teh the |", field.toString())
    }

    @Test
    fun unknownWordsAreLearnedOnTheirSecondUse() {
        val lexicon = learning()
        val field = FakeTextField("")
        type(field, "Kayleigh said ")
        assertFalse("Kayleigh" in lexicon)
        type(field, "Kayleigh laughed.")
        assertTrue("Kayleigh" in lexicon)

        // Learned words are suggested and counted as they're used: learned, typed again, and learned here
        type(field, " Kayl")
        assertTrue(session.chips.any { it?.word == "Kayleigh" })
        type(field, "eigh ")
        assertEquals(3, lexicon.uses("Kayleigh"))
    }

    @Test
    fun typosAreNotLearned() {
        val lexicon = learning()
        val field = FakeTextField("")
        type(field, "teh cat and teh dog @kayleigh @kayleigh www.kayleigh www.kayleigh ")
        assertEquals(0, lexicon.size)
    }

    @Test
    fun stickyLearnedWordsAreNotCorrected() {
        autocorrecting()
        val lexicon = learning()
        lexicon.learn("wuick", EngineConstants.STICKY_USES)
        val field = FakeTextField("")
        type(field, "wuick ")
        assertEquals("wuick |", field.toString())
    }

    @Test
    fun nothingIsLearnedWhenLearningIsOff() {
        autocorrecting()
        val lexicon = learning()
        session.isLearningEnabled = false
        val field = FakeTextField("")
        type(field, "yeet yeet teh ")
        session.undoCorrection(field)
        assertEquals(0, lexicon.size)
    }
}
