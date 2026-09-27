package org.fossify.keyboard.suggestions

/** A suggested word, already in the case of what was typed. */
class ScoredWord(
    val word: String,
    val score: Float,
    val cost: Float,
    val flags: Int,
    val isExact: Boolean,
    val isCompletion: Boolean,
)

/**
 * Suggestions for [typed], best first. [unknownWordScore] is the score [typed] would get as a rare word missing from
 * the dictionary, and [maxCost] is the highest correction cost allowed for its length.
 */
class Suggestions(
    val typed: String,
    val candidates: List<ScoredWord>,
    val typedIsWord: Boolean,
    val unknownWordScore: Float = Float.MAX_VALUE,
    val maxCost: Float = 0f,
) {
    val words: List<String>
        get() = candidates.take(EngineConstants.MAX_SUGGESTIONS).map { it.word }

    companion object {
        fun none(typed: String) = Suggestions(typed, emptyList(), typedIsWord = false)
    }
}

/**
 * Finds exact matches, completions and corrections for a typed word in the dictionary and the words the user taught
 * the keyboard, and ranks them. Results for recently typed words are cached, as the same prefixes are queried again
 * when the user deletes chars.
 */
class SuggestionEngine(
    private val trie: ArrayTrie,
    geometry: KeyboardGeometry = KeyboardGeometry.QWERTY,
    private val ranker: Ranker = Ranker(),
) {
    private val search = FuzzySearch(trie)
    private val cache = object : LinkedHashMap<String, Suggestions>(EngineConstants.CACHE_SIZE, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Suggestions>) =
            size > EngineConstants.CACHE_SIZE
    }

    var errorModel: ErrorModel = KeyboardErrorModel(geometry)
        set(value) {
            field = value
            cache.clear()
        }

    var geometry: KeyboardGeometry = geometry
        set(value) {
            field = value
            errorModel = KeyboardErrorModel(value)
        }

    /** The words the user taught the keyboard, suggested along with the dictionary. */
    var userLexicon: UserLexicon? = null
        set(value) {
            field = value
            cache.clear()
        }

    private var cachedUserLexiconVersion = 0

    fun isWord(word: CharSequence) = word in trie

    /** Returns how often the user confirmed [word], or 0 if it wasn't learned. */
    fun userWordUses(word: CharSequence) = userLexicon?.uses(word) ?: 0

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
        if (key.isNullOrEmpty()) return Suggestions.none(typed)

        val symbols = IntArray(key.length) { Alphabet.symbolOf(key[it]) }
        val maxCost = errorModel.maxCost(key.length)
        val candidates = search.corrections(symbols, errorModel, maxCost, withCompletions = true)
        val pattern = CaseMapper.patternOf(typed)
        val best = HashMap<String, ScoredWord>()
        fun add(surface: String, candidateKey: String, freq: Int, cost: Float, flags: Int, isCompletion: Boolean) {
            val isExact = !isCompletion && candidateKey == key
            val score = ranker.score(key, candidateKey, freq, cost, isExact)
            val word = CaseMapper.apply(surface, pattern)
            val previous = best[word]
            if (previous == null || previous.score < score) {
                best[word] = ScoredWord(word, score, cost, flags, isExact, isCompletion)
            }
        }

        for (candidate in candidates) {
            trie.forEachWord(candidate.node, candidate.key) { surface, freq, flags ->
                if (flags and ArrayTrie.OFFENSIVE == 0) {
                    add(surface, candidate.key, freq, candidate.cost, flags, candidate.isCompletion)
                }
            }
        }

        userLexicon?.candidates(symbols, errorModel, maxCost)?.forEach {
            add(it.word, it.key, it.freq, it.cost, 0, it.isCompletion)
        }

        val ranked = best.values.sortedByDescending { it.score }
        return Suggestions(
            typed = typed,
            candidates = ranked,
            typedIsWord = key in trie,
            unknownWordScore = ranker.score(key, key, EngineConstants.UNKNOWN_WORD_FREQ, 0f, isExact = false),
            maxCost = maxCost,
        )
    }

    private companion object {
        const val LOAD_FACTOR = 0.75f
    }
}
