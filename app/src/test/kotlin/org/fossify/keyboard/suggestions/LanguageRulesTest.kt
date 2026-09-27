package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The engine with the rules of languages other than English, on small dictionaries. */
class LanguageRulesTest {

    private val accents = LanguageRules(restoresAccents = true)
    private val french = LanguageRules(
        restoresAccents = true,
        elisionPrefixes = setOf("l", "c", "qu", "jusqu"),
        splitsAtHyphen = true,
        extraSeparators = "»",
    )

    private fun trieOf(vararg words: Pair<String, Int>, offensive: Set<String> = emptySet()): ArrayTrie {
        val entries = words.map { (surface, freq) ->
            val key = Alphabet.fold(surface)!!
            val flags = if (surface in offensive) ArrayTrie.OFFENSIVE else 0
            DictionaryEntry(key, freq, flags, surface.takeIf { it != key })
        }
        return TrieBuilder.build(entries.sortedBy { it.key })
    }

    private fun correct(engine: SuggestionEngine, typed: String) =
        AutocorrectPolicy.correctionFor(typed, engine.suggest(typed), emptySet(), isStickyUserWord = false)?.word

    @Test
    fun rulesAreLookedUpByLocale() {
        assertSame(LanguageRules.ENGLISH, LanguageRules.forLocale("en_US"))
        assertSame(LanguageRules.DEFAULT, LanguageRules.forLocale("xx_XX"))
        assertTrue(AutocorrectPolicy.isCorrectable("i", "i", LanguageRules.ENGLISH))
        assertFalse(AutocorrectPolicy.isCorrectable("i", "i", LanguageRules.DEFAULT))
    }

    @Test
    fun ligaturesFoldIntoTwoLetters() {
        assertEquals("strasse", Alphabet.fold("Straße"))
        assertEquals("strasse", Alphabet.fold("STRASSE"))
        assertEquals("strasse", Alphabet.fold("STRAẞE"))
        assertEquals("coeur", Alphabet.fold("cœur"))
        assertEquals("aesop", Alphabet.fold("Æsop"))
        assertEquals('s', Alphabet.fold('ß'))
        assertTrue(Alphabet.isWordChar('œ'))
    }

    @Test
    fun wordsWithLigaturesAreFoundHoweverTheyAreTyped() {
        val trie = trieOf("Straße" to 150, "Masse" to 140, "Maße" to 120, "cœur" to 160)
        assertTrue("Straße" in trie)
        assertTrue("strasse" in trie)
        assertTrue("coeur" in trie)

        val engine = SuggestionEngine(trie, rules = accents)
        assertEquals("Straße", correct(engine, "strasse"))
        assertEquals("cœur", correct(engine, "coeur"))
        assertEquals("STRASSE", engine.suggest("STRASSE").words.first())
        assertNull(correct(engine, "Masse"))
        assertNull(correct(engine, "Maße"))
    }

    @Test
    fun inoffensiveSpellingsOfAKeyArePrimary() {
        val trie = trieOf("ano" to 200, "año" to 150, offensive = setOf("ano"))
        assertEquals("año", trie.primarySurface(trie.find("ano"), "ano"))
        assertEquals(listOf("año"), SuggestionEngine(trie, rules = accents).suggest("an").words)
    }

    @Test
    fun accentsAreOnlyRestoredWhereTheLanguageDoesIt() {
        val trie = trieOf("não" to 200, "no" to 180, "esta" to 190, "está" to 200, "café" to 120)
        val restoring = SuggestionEngine(trie, rules = accents)
        assertFalse(restoring.isWord("nao"))
        assertTrue(restoring.isWord("não"))
        assertTrue(restoring.isWord("NÃO"))
        assertTrue(restoring.isWord("esta"))
        assertEquals("não", correct(restoring, "nao"))
        assertEquals("Não", correct(restoring, "Nao"))
        assertNull(correct(restoring, "esta"))
        assertNull(correct(restoring, "está"))

        val english = SuggestionEngine(trie)
        assertTrue(english.isWord("nao"))
        assertNull(correct(english, "nao"))
        assertNull(correct(english, "cafe"))
    }

    @Test
    fun theSpellingTypedComesFirst() {
        val engine = SuggestionEngine(trieOf("ou" to 200, "où" to 150), rules = accents)
        assertEquals(listOf("où", "ou"), engine.suggest("où").words)
        assertEquals(listOf("ou", "où"), engine.suggest("ou").words)
    }

    @Test
    fun learnedWordsAreNotSpelledWords() {
        val engine = SuggestionEngine(trieOf("zorro" to 120), rules = accents)
        engine.userLexicon = UserLexicon().apply { learn("zorp", 2) }
        val suggestions = engine.suggest("zorp")
        assertFalse(engine.isWord("zorp"))
        assertFalse(suggestions.typedIsWord)
        assertTrue(suggestions.candidates.first { it.word == "zorp" }.isLearned)
        assertNotEquals("zorp", correct(engine, "zorp"))
    }

    @Test
    fun compoundsOfTwoWordsAreValid() {
        val trie = trieOf("Haus" to 200, "Tür" to 170, "Arbeit" to 180, "Zimmer" to 170)
        val german = SuggestionEngine(trie, rules = LanguageRules(restoresAccents = true, compounds = true))
        assertTrue(german.isWord("Zimmertür"))
        assertTrue(german.isWord("zimmertür"))
        assertTrue(german.isWord("Arbeitszimmer"))
        assertFalse(german.isWord("Zimmertur"))
        assertFalse(german.isWord("Zimmerxtür"))
        assertFalse(SuggestionEngine(trie, rules = accents).isWord("Zimmertür"))
    }

    @Test
    fun elisionsAndHyphensAreSplitOff() {
        val known = listOf("aujourd'hui", "homme", "être", "à")
        fun lookedUp(word: String) =
            CurrentWord(word, word).lookedUp(french) { typed -> known.any { it.startsWith(typed.lowercase()) } }?.word

        assertEquals("homme", lookedUp("l'homme"))
        assertEquals("homme", lookedUp("L’homme"))
        assertEquals("hmome", lookedUp("l'hmome"))
        assertEquals("aujourd'hui", lookedUp("aujourd'hui"))
        assertEquals("aujourd'h", lookedUp("aujourd'h"))
        assertEquals("etre", lookedUp("peut-etre"))
        assertEquals("à", lookedUp("jusqu'à"))
        assertEquals("quelqu'un", lookedUp("quelqu'un"))
        assertNull(lookedUp("l'"))
        assertNull(lookedUp("peut-"))

        val current = CurrentWord("l'homme", "(l'homme")
        assertEquals("(l'homme", current.lookedUp(french) { false }!!.token)
        assertSame(current, current.lookedUp(LanguageRules.ENGLISH) { false })
    }

    @Test
    fun onlyTheWordAfterAnElisionOrHyphenIsCorrected() {
        val trie = trieOf("homme" to 180, "être" to 190, "peut" to 200, "c'est" to 210, "est" to 200)
        val session = SuggestionSession().apply {
            engine = SuggestionEngine(trie, rules = french)
            isEnabled = true
            isAutocorrectEnabled = true
        }
        val field = FakeTextField("")
        for (char in "l'hmome peut-etre cest «l'hmome»") {
            if (!session.onCharTyped(field, char)) field.type(char.toString())
            session.refresh(field)
        }

        assertEquals("l'homme peut-être c'est «l'homme»|", field.toString())
    }

    @Test
    fun lettersKeepTheirOwnKeysBesideAccentedOnes() {
        val spanish = KeyboardGeometry.fromRows(listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm"), listOf(0f, 0f, 1f))
        val n = Alphabet.symbolOf('n')
        assertTrue(spanish.areNeighbours(n, Alphabet.symbolOf('m')))
        assertFalse(spanish.areNeighbours(n, Alphabet.symbolOf('l')))

        // An accented key places a letter that has no key of its own
        val withoutE = KeyboardGeometry.fromRows(listOf("qwrtyuiop", "asdfghjklé", "zxcvbnm"), listOf(0f, 0f, 1f))
        assertTrue(withoutE.areNeighbours(Alphabet.symbolOf('e'), Alphabet.symbolOf('l')))
    }

    @Test
    fun learnedCapitalsAndAccentsCanBeKept() {
        assertEquals("kita", UserLexicon().apply { learn("Kita") }.learn("kita")!!.word)
        assertEquals("Kita", UserLexicon(keepsCapitals = true).apply { learn("Kita") }.learn("kita")!!.word)
        assertEquals("sao", UserLexicon().apply { learn("São") }.learn("sao")!!.word)
        assertEquals("São", UserLexicon(keepsAccents = true).apply { learn("São") }.learn("sao")!!.word)
    }
}
