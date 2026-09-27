package org.fossify.keyboard.suggestions

import java.util.Locale
import kotlin.random.Random

/** A typed word and the word that was meant. */
data class TypoCase(val typed: String, val intended: String)

/** Something that suggests words for a typed word, best first. */
fun interface SuggestionSystem {
    fun suggest(typed: String): List<String>
}

/** Full-scan baselines are slow but stateless, so they are evaluated in parallel. */
class ParallelSystem(private val system: SuggestionSystem) : SuggestionSystem by system

class EvaluationResult(val top1: Float, val top3: Float, val cases: Int)

/** Shares of cases where autocorrect replaced the typed word with the intended word, or with another word. */
class AutocorrectResult(val right: Float, val wrong: Float)

/** Datasets and baselines for comparing suggestion systems. Everything is seeded, so results are reproducible. */
object Evaluation {
    /** How much worse a wrong correction is than a missed one: it has to be noticed and undone. */
    const val WRONG_PENALTY = 3f

    private const val SEED = 42
    private const val TWO_EDIT_SHARE = 0.2
    private const val SYNTHETIC_SOURCE_WORDS = 5000

    private val dictionary = TestDictionary.entries.filter { it.flags and ArrayTrie.OFFENSIVE == 0 }

    /** Dictionary words, most frequent first. */
    private val wordsByFreq = dictionary.sortedByDescending { it.freq }.map { it.surface ?: it.key }

    fun loadCases(resource: String): List<TypoCase> {
        val stream = requireNotNull(Evaluation::class.java.getResourceAsStream(resource)) { resource }
        return stream.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.split("\t").let { fields -> TypoCase(fields[0], fields[1]) } }
    }

    fun loadWords(resource: String): List<String> {
        val stream = requireNotNull(Evaluation::class.java.getResourceAsStream(resource)) { resource }
        return stream.bufferedReader().readLines().filter { it.isNotBlank() && !it.startsWith("#") }
    }

    /** Keeps the cases that a dictionary-based system can fix at all. */
    fun usable(cases: List<TypoCase>) =
        cases.filter { it.typed !in TestDictionary.trie && it.intended in TestDictionary.trie }

    /** Dev and test halves, split by intended word so the same word never lands in both. */
    fun split(cases: List<TypoCase>, dev: Boolean) = cases.filter { (it.intended.hashCode() and 1 == 0) == dev }

    /** Adjacent-key substitutions, transpositions, omissions, neighbour double hits and doublings of common words. */
    fun syntheticCases(count: Int): List<TypoCase> {
        val random = Random(SEED)
        val sources = wordsByFreq
            .filter { it.length >= 2 && it.all { c -> c in 'a'..'z' } }
            .take(SYNTHETIC_SOURCE_WORDS)
        val cases = LinkedHashSet<TypoCase>()
        var attempts = 0
        while (cases.size < count && attempts++ < count * 10) {
            val word = sources[random.nextInt(sources.size)]
            var typed = slip(word, random)
            if (random.nextDouble() < TWO_EDIT_SHARE) typed = slip(typed, random)
            if (typed != word && typed.isNotEmpty() && typed !in TestDictionary.trie) {
                cases.add(TypoCase(typed, word))
            }
        }

        return cases.toList()
    }

    /** Prefixes of common words, which should be completed. */
    fun completionCases(count: Int): List<TypoCase> {
        val random = Random(SEED + 2)
        val sources = wordsByFreq.filter { it.length >= MIN_COMPLETED_LENGTH && it.all { c -> c in 'a'..'z' } }
            .take(SYNTHETIC_SOURCE_WORDS)
        return sources.shuffled(random).take(count).map { word ->
            TypoCase(word.substring(0, 2 + random.nextInt(word.length - 2)), word)
        }
    }

    /** Common words that must be left alone. */
    fun validWords(count: Int): List<String> {
        val random = Random(SEED + 1)
        return wordsByFreq.take(count * 10).shuffled(random).take(count)
    }

    fun evaluate(system: SuggestionSystem, cases: List<TypoCase>): EvaluationResult {
        var top1 = 0
        var top3 = 0
        val suggestions = suggestAll(system, cases.map { it.typed })
        cases.forEachIndexed { i, case ->
            val words = suggestions[i].take(3).map { it.lowercase(Locale.ROOT) }
            val intended = case.intended.lowercase(Locale.ROOT)
            if (words.firstOrNull() == intended) top1++
            if (intended in words) top3++
        }

        return EvaluationResult(top1 / cases.size.toFloat(), top3 / cases.size.toFloat(), cases.size)
    }

    fun autocorrect(
        engine: SuggestionEngine,
        cases: List<TypoCase>,
        margin: Float = EngineConstants.AUTOCORRECT_MARGIN,
    ): AutocorrectResult {
        var right = 0
        var wrong = 0
        for (case in cases) {
            val suggestions = engine.suggest(case.typed)
            val correction = AutocorrectPolicy.correctionFor(case.typed, suggestions, emptySet(), false, margin)
            when {
                correction == null -> {}
                correction.word.equals(case.intended, ignoreCase = true) -> right++
                else -> wrong++
            }
        }

        return AutocorrectResult(right / cases.size.toFloat(), wrong / cases.size.toFloat())
    }

    /** Share of words that autocorrect changes into a different word, ignoring case-only fixes. */
    fun autocorrectedWords(
        engine: SuggestionEngine,
        words: List<String>,
        margin: Float = EngineConstants.AUTOCORRECT_MARGIN,
    ): Float {
        return words.count { word ->
            val correction = AutocorrectPolicy.correctionFor(word, engine.suggest(word), emptySet(), false, margin)
            correction != null && !correction.word.equals(word, ignoreCase = true)
        } / words.size.toFloat()
    }

    /** Share of valid words whose best suggestion is a different word. */
    fun falseCorrectionRate(system: SuggestionSystem, words: List<String>): Float {
        val suggestions = suggestAll(system, words)
        val changed = words.indices.count {
            suggestions[it].firstOrNull()?.lowercase(Locale.ROOT) != words[it].lowercase(Locale.ROOT)
        }
        return changed / words.size.toFloat()
    }

    private fun suggestAll(system: SuggestionSystem, queries: List<String>): List<List<String>> {
        return if (system is ParallelSystem) {
            queries.parallelStream().map { system.suggest(it) }.toList()
        } else {
            queries.map { system.suggest(it) }
        }
    }

    /** Returns the 50th and 95th percentile of the time a query takes, in microseconds. */
    fun latency(system: SuggestionSystem, allQueries: List<String>): Pair<Float, Float> {
        val queries = if (system is ParallelSystem) {
            allQueries.shuffled(Random(SEED)).take(SLOW_LATENCY_QUERIES)
        } else {
            allQueries
        }
        queries.forEach { system.suggest(it) }
        val times = queries.map { query ->
            val start = System.nanoTime()
            system.suggest(query)
            (System.nanoTime() - start) / NANOS_PER_MICRO
        }.sorted()

        return times[times.size / 2] to times[(times.size * P95).toInt()]
    }

    private fun slip(word: String, random: Random): String {
        val i = random.nextInt(word.length)
        val c = word[i]
        return when (random.nextInt(SLIP_KINDS)) {
            0 -> word.replaceRange(i, i + 1, neighbourOf(c, random).toString())
            1 -> if (i + 1 < word.length) word.substring(0, i) + word[i + 1] + c + word.substring(i + 2) else word
            2 -> if (word.length > 1) word.removeRange(i, i + 1) else word
            3 -> word.substring(0, i + 1) + neighbourOf(c, random) + word.substring(i + 1)
            else -> word.substring(0, i + 1) + c + word.substring(i + 1)
        }
    }

    private fun neighbourOf(c: Char, random: Random): Char {
        val geometry = KeyboardGeometry.QWERTY
        val symbol = Alphabet.symbolOf(c)
        if (symbol == Alphabet.NO_SYMBOL) return c
        val neighbours = ('a'..'z').filter { geometry.areNeighbours(symbol, Alphabet.symbolOf(it)) }
        return if (neighbours.isEmpty()) c else neighbours[random.nextInt(neighbours.size)]
    }

    /** The approach of FossifyOrg/Keyboard#346: prefix matches by frequency, else Levenshtein ≤ 2 by distance. */
    val levenshteinBaseline = ParallelSystem { typed ->
        val key = typed.lowercase(Locale.ROOT)
        val completions = dictionary.filter { (it.surface ?: it.key).lowercase(Locale.ROOT).startsWith(key) }
            .sortedByDescending { it.freq }
        if (completions.isNotEmpty()) {
            completions.map { it.surface ?: it.key }
        } else {
            dictionary.filter { kotlin.math.abs(it.key.length - key.length) <= 2 }
                .map { it to levenshtein(key, it.key) }
                .filter { it.second <= 2 }
                .sortedWith(compareBy<Pair<DictionaryEntry, Int>> { it.second }.thenByDescending { it.first.freq })
                .map { it.first.surface ?: it.first.key }
        }
    }

    /** Ranks every word by Jaro-Winkler similarity plus a little frequency. */
    val jaroWinklerBaseline = ParallelSystem { typed ->
        val key = Alphabet.fold(typed) ?: typed
        dictionary.map {
            it to JaroWinkler.similarity(key, it.key) + JW_BASELINE_FREQ_WEIGHT * it.freq / EngineConstants.FREQ_SCALE
        }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first.surface ?: it.first.key }
    }

    fun engineSystem(errorModel: ErrorModel? = null, ranker: Ranker = Ranker()): SuggestionSystem {
        val engine = SuggestionEngine(TestDictionary.trie, ranker = ranker)
        if (errorModel != null) engine.errorModel = errorModel
        return SuggestionSystem { engine.suggest(it).words }
    }

    fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(substitution, previous[j] + 1, current[j - 1] + 1)
            }

            previous = current.also { current = previous }
        }

        return previous[b.length]
    }

    private const val SLIP_KINDS = 5
    private const val MIN_COMPLETED_LENGTH = 4
    private const val SLOW_LATENCY_QUERIES = 200
    private const val NANOS_PER_MICRO = 1000f
    private const val P95 = 0.95
    private const val JW_BASELINE_FREQ_WEIGHT = 0.05f
}

/** Every edit costs 1, the textbook optimal string alignment distance. */
object UniformErrorModel : ErrorModel {
    override val transposition = 1f
    override val firstLetterFactor = 1f

    override fun substitution(typed: IntArray, i: Int, intended: Int) = 1f

    override fun insertion(typed: IntArray, i: Int) = 1f

    override fun deletion(intended: Int, previous: Int) = 1f
}
