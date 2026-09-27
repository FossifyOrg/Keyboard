package org.fossify.keyboard.suggestions

/** A suggested word, already in the case of what was typed. [isLearned] marks words the user taught the keyboard. */
class ScoredWord(
    val word: String,
    val score: Float,
    val cost: Float,
    val flags: Int,
    val isExact: Boolean,
    val isCompletion: Boolean,
    val isLearned: Boolean = false,
)

/**
 * Suggestions for [typed], best first. [unknownWordScore] is the score [typed] would get as a rare word missing from
 * the dictionary, [maxCost] is the highest correction cost allowed for its length and [rules] are those of the
 * dictionary's language.
 */
class Suggestions(
    val typed: String,
    val candidates: List<ScoredWord>,
    val typedIsWord: Boolean,
    val unknownWordScore: Float = Float.MAX_VALUE,
    val maxCost: Float = 0f,
    val rules: LanguageRules = LanguageRules.ENGLISH,
) {
    val words: List<String>
        get() = candidates.take(EngineConstants.MAX_SUGGESTIONS).map { it.word }

    companion object {
        fun none(typed: String, rules: LanguageRules = LanguageRules.ENGLISH) =
            Suggestions(typed, emptyList(), typedIsWord = false, rules = rules)
    }
}

/**
 * Finds exact matches, completions and corrections for a typed word in the dictionary and the words the user taught
 * the keyboard, and ranks them with the weights and [rules] of the dictionary's language. Results for recently typed
 * words are cached, as the same prefixes are queried again when the user deletes chars.
 */
class SuggestionEngine(
    private val trie: ArrayTrie,
    geometry: KeyboardGeometry = KeyboardGeometry.QWERTY,
    val rules: LanguageRules = LanguageRules.ENGLISH,
    private val ranker: Ranker = Ranker(rules.weights),
) {
    private val search = FuzzySearch(trie)
    private val cache = object : LinkedHashMap<String, Suggestions>(EngineConstants.CACHE_SIZE, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Suggestions>) =
            size > EngineConstants.CACHE_SIZE
    }

    var errorModel: ErrorModel = KeyboardErrorModel(geometry, rules.weights)
        set(value) {
            field = value
            cache.clear()
        }

    var geometry: KeyboardGeometry = geometry
        set(value) {
            field = value
            errorModel = KeyboardErrorModel(value, rules.weights)
        }

    /** The words the user taught the keyboard, suggested along with the dictionary. */
    var userLexicon: UserLexicon? = null
        set(value) {
            field = value
            cache.clear()
        }

    private var cachedUserLexiconVersion = 0

    /**
     * Returns whether [word] is spelled like a dictionary word. Without [LanguageRules.restoresAccents] any word with
     * a dictionary key is, so the English cafe is a word although only café is in the dictionary. With it, the word
     * must match a spelling in the dictionary but for its case and the kind of apostrophe. Offensive words count, as
     * they're valid when typed, and so do compounds of two words in languages with [LanguageRules.compounds].
     * Learned words don't, so they're corrected like unknown words until they stick.
     */
    fun isWord(word: CharSequence): Boolean {
        if (!rules.restoresAccents) return word in trie
        return isSpelledWord(word) || isCompound(word)
    }

    /** Returns whether [word] is a dictionary word or the start of one, whatever its accents. */
    fun isKnownPrefix(word: CharSequence) = trie.find(word) != ArrayTrie.NO_NODE

    /** Returns how often the user confirmed [word], or 0 if it wasn't learned. */
    fun userWordUses(word: CharSequence) = userLexicon?.uses(word) ?: 0

    private fun isSpelledWord(word: CharSequence): Boolean {
        val node = trie.find(word)
        if (node == ArrayTrie.NO_NODE || !trie.isTerminal(node)) return false

        val key = Alphabet.fold(word) ?: return false
        var found = false
        trie.forEachWord(node, key) { surface, _, _ -> if (!found) found = isSpelledAs(surface, word) }
        return found
    }

    /**
     * A compound of two words, spelled as in the dictionary but for their case, like Haustür (Haus + Tür) or
     * Arbeitszimmer (Arbeit + s + Zimmer).
     */
    private fun isCompound(word: CharSequence): Boolean {
        if (!rules.compounds || word.length < EngineConstants.MIN_COMPOUND_LENGTH) return false

        val minPart = EngineConstants.MIN_COMPOUND_PART_LENGTH
        for (split in minPart..word.length - minPart) {
            if (!isSpelledWord(word.subSequence(split, word.length))) continue
            val head = word.subSequence(0, split)
            if (isSpelledWord(head)) return true

            val hasLinkingS = head.last().lowercaseChar() == LINKING_S && head.length > minPart
            if (hasLinkingS && isSpelledWord(head.subSequence(0, head.length - 1))) return true
        }

        return false
    }

    fun suggest(typed: String): Suggestions {
        val userLexiconVersion = userLexicon?.version ?: 0
        if (userLexiconVersion != cachedUserLexiconVersion) {
            cachedUserLexiconVersion = userLexiconVersion
            cache.clear()
        }

        return cache.getOrPut(typed) { compute(typed) }
    }

    private fun compute(typed: String): Suggestions {
        val key = Alphabet.fold(typed)
        if (key.isNullOrEmpty()) return Suggestions.none(typed, rules)

        val symbols = IntArray(key.length) { Alphabet.symbolOf(key[it]) }
        val maxCost = errorModel.maxCost(key.length)
        val candidates = search.corrections(symbols, errorModel, maxCost, withCompletions = true)
        val pattern = CaseMapper.patternOf(typed)
        val best = HashMap<String, ScoredWord>()
        fun add(candidate: Candidate, surface: String, freq: Int, flags: Int, isLearned: Boolean) {
            val isExact = !candidate.isCompletion && candidate.key == key
            // The spelling typed comes first among those of the typed key, like où before ou
            val boost = if (isExact && rules.restoresAccents && isSpelledAs(surface, typed)) {
                EngineConstants.TYPED_SPELLING_BONUS
            } else {
                0f
            }

            val score = ranker.score(key, candidate.key, freq, candidate.cost, isExact, boost)
            val word = CaseMapper.apply(surface, pattern)
            val previous = best[word]
            if (previous == null || previous.score < score) {
                best[word] = ScoredWord(word, score, candidate.cost, flags, isExact, candidate.isCompletion, isLearned)
            }
        }

        for (candidate in candidates) {
            trie.forEachWord(candidate.node, candidate.key) { surface, freq, flags ->
                if (flags and ArrayTrie.OFFENSIVE == 0) {
                    add(candidate, surface, freq, flags, isLearned = false)
                }
            }
        }

        userLexicon?.candidates(symbols, errorModel, maxCost)?.forEach {
            add(Candidate(it.key, ArrayTrie.NO_NODE, it.cost, it.isCompletion), it.word, it.freq, 0, isLearned = true)
        }

        val ranked = best.values.sortedByDescending { it.score }
        return Suggestions(
            typed = typed,
            candidates = ranked,
            typedIsWord = isWord(typed),
            unknownWordScore = ranker.score(key, key, EngineConstants.UNKNOWN_WORD_FREQ, 0f, isExact = false),
            maxCost = maxCost,
            rules = rules,
        )
    }

    private companion object {
        const val LOAD_FACTOR = 0.75f
        const val LINKING_S = 's'

        /** Whether two spellings are the same but for their case and the kind of apostrophe. */
        fun isSpelledAs(surface: CharSequence, word: CharSequence): Boolean {
            if (surface.length != word.length) return false
            for (i in surface.indices) {
                val a = surface[i]
                val b = word[i]
                val same = a.equals(b, ignoreCase = true) ||
                    (Alphabet.fold(a) == Alphabet.APOSTROPHE && Alphabet.fold(b) == Alphabet.APOSTROPHE)
                if (!same) return false
            }

            return true
        }
    }
}
