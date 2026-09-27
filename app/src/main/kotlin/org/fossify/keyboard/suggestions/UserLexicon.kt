package org.fossify.keyboard.suggestions

import kotlin.math.ln
import kotlin.math.roundToInt

/** A learned word that a typed word could be a correction or the start of. */
class UserCandidate(val word: String, val key: String, val freq: Int, val cost: Float, val isCompletion: Boolean)

/**
 * The words the user taught the keyboard, with how often they were confirmed. There are few enough of them to be
 * compared with the typed word one by one. The least recently confirmed words make room for new ones.
 */
class UserLexicon(private val capacity: Int = EngineConstants.MAX_USER_WORDS) {
    private val words = object : LinkedHashMap<String, Entry>(INITIAL_CAPACITY, LOAD_FACTOR, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>) = size > capacity
    }

    /** Changes whenever the words change, so suggestions made before can be dropped. */
    var version = 0
        private set

    val size: Int
        get() = words.size

    operator fun contains(word: CharSequence): Boolean = uses(word) > 0

    /** Returns how often [word] was confirmed, or 0 if it wasn't learned. */
    fun uses(word: CharSequence): Int {
        val key = Alphabet.fold(word) ?: return 0
        return words[key]?.count ?: 0
    }

    /**
     * Counts [uses] confirmations of [word] and returns the learned word, or null if it doesn't fit the alphabet. A
     * word first learned with a capital is kept in lowercase once it's used in lowercase, as the capital was probably
     * the sentence's.
     */
    fun learn(word: String, uses: Int = 1): LearnedWord? {
        val key = Alphabet.fold(word) ?: return null
        val entry = words.remove(key)
        val surface = if (entry == null || word == word.lowercase()) word else entry.word
        val learned = Entry(surface, (entry?.count ?: 0) + uses)
        words[key] = learned
        version++
        return LearnedWord(learned.word, learned.count, replaced = entry?.word?.takeIf { it != surface })
    }

    /** Adds a word learned before. If it was learned again meanwhile, the uses are added up. */
    fun add(word: String, count: Int) {
        val key = Alphabet.fold(word) ?: return
        val entry = words[key]
        words[key] = if (entry == null) Entry(word, count) else Entry(entry.word, entry.count + count)
        version++
    }

    fun clear() {
        words.clear()
        version++
    }

    /**
     * Returns the learned words that the [typed] symbols could be meant as, or the start of, within [maxCost]. Short
     * typed words only complete learned words they're an exact prefix of, like with dictionary words.
     */
    fun candidates(typed: IntArray, model: ErrorModel, maxCost: Float): List<UserCandidate> {
        val results = ArrayList<UserCandidate>()
        val aligner = Aligner(model, typed, maxCost)
        for ((key, entry) in words) {
            val alignment = aligner.align(entry.symbols)
            when {
                alignment.cost <= maxCost -> {
                    results.add(UserCandidate(entry.word, key, freqOf(entry.count), alignment.cost, false))
                }

                isCompletable(typed.size, alignment.prefixCost) -> {
                    val cost = alignment.prefixCost + EngineConstants.COMPLETION_COST
                    results.add(UserCandidate(entry.word, key, freqOf(entry.count), cost, isCompletion = true))
                }
            }
        }

        return results
    }

    private fun isCompletable(typedLength: Int, prefixCost: Float): Boolean {
        val longEnough = typedLength >= EngineConstants.MIN_FUZZY_PREFIX_LENGTH || prefixCost == 0f
        return longEnough && prefixCost <= EngineConstants.MAX_PREFIX_COST
    }

    /** The frequency a learned word is ranked with: [EngineConstants.USER_WORD_ZIPF] + ln(uses), like a Zipf value. */
    private fun freqOf(count: Int): Int {
        val zipf = EngineConstants.USER_WORD_ZIPF + ln(count.toFloat())
        return (zipf * EngineConstants.FREQ_SCALE).roundToInt().coerceAtMost(ArrayTrie.MAX_FREQ)
    }

    private class Entry(val word: String, val count: Int) {
        val symbols = Alphabet.fold(word).orEmpty().let { key -> IntArray(key.length) { Alphabet.symbolOf(key[it]) } }
    }

    private companion object {
        const val INITIAL_CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
    }
}

/** A word as learned, with the spelling it replaced if it changed. */
class LearnedWord(val word: String, val count: Int, val replaced: String?)
