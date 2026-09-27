package org.fossify.keyboard.suggestions

import java.text.Normalizer
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

/**
 * Datasets and baselines for comparing suggestion systems on the dictionary and layout of a [language]. Everything is
 * seeded, so results are reproducible.
 */
class Evaluation(private val language: LanguageCase) {
    /** Tells words from typos like the engine does, so accentless typos are typos where accents are restored. */
    private val engine = language.engine()

    private val dictionary = language.entries.filter { it.flags and ArrayTrie.OFFENSIVE == 0 }

    /** Dictionary words, most frequent first. */
    private val wordsByFreq = dictionary.sortedByDescending { it.freq }.map { it.surface ?: it.key }

    /** Loads the typo cases in the file [name] of the language's datasets. */
    fun cases(name: String) = loadCases(language.resource(name))

    /** Loads the words in the file [name] of the language's datasets, or none if it has no such file. */
    fun words(name: String) = if (language.hasResource(name)) loadWords(language.resource(name)) else emptyList()

    /** Keeps the cases that a dictionary-based system can fix at all. */
    fun usable(cases: List<TypoCase>) = cases.filter { !engine.isWord(it.typed) && engine.isWord(it.intended) }

    /** Adjacent-key substitutions, transpositions, omissions, neighbour double hits and doublings of common words. */
    fun syntheticCases(count: Int): List<TypoCase> {
        val random = Random(SEED)
        val sources = wordsByFreq
            .filter { it.length >= 2 && language.isSourceWord(it) }
            .take(SYNTHETIC_SOURCE_WORDS)
        val cases = LinkedHashSet<TypoCase>()
        var attempts = 0
        while (cases.size < count && attempts++ < count * 10) {
            val word = sources[random.nextInt(sources.size)]
            var typed = slip(word, random)
            if (random.nextDouble() < TWO_EDIT_SHARE) typed = slip(typed, random)
            if (typed != word && typed.isNotEmpty() && !engine.isWord(typed)) {
                cases.add(TypoCase(typed, word))
            }
        }

        return cases.toList()
    }

    /** Common words typed without their accents (não as nao, Straße as Strasse), some with a slip too. */
    fun accentCases(count: Int): List<TypoCase> {
        val random = Random(SEED + ACCENTS_SEED_OFFSET)
        val sources = wordsByFreq
            .filter { it.length >= 2 && language.isSourceWord(it) && stripAccents(it) != it }
            .take(SYNTHETIC_SOURCE_WORDS)
        if (sources.isEmpty()) return emptyList()

        val cases = LinkedHashSet<TypoCase>()
        var attempts = 0
        while (cases.size < count && attempts++ < count * 10) {
            val word = sources[random.nextInt(sources.size)]
            var typed = stripAccents(word)
            if (random.nextDouble() < TWO_EDIT_SHARE) typed = slip(typed, random)
            if (typed.isNotEmpty() && !engine.isWord(typed)) {
                cases.add(TypoCase(typed, word))
            }
        }

        return cases.toList()
    }

    /** Prefixes of common words, which should be completed. */
    fun completionCases(count: Int): List<TypoCase> {
        val random = Random(SEED + 2)
        val sources = wordsByFreq.filter { it.length >= MIN_COMPLETED_LENGTH && language.isSourceWord(it) }
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
        val geometry = language.geometry
        val symbol = Alphabet.symbolOf(Alphabet.fold(c))
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

    fun engineSystem(
        errorModel: ErrorModel? = null,
        ranker: Ranker = Ranker(language.rules.weights),
    ): SuggestionSystem {
        val engine = SuggestionEngine(language.trie, language.geometry, language.rules, ranker)
        if (errorModel != null) engine.errorModel = errorModel
        return SuggestionSystem { engine.suggest(it).words }
    }

    companion object {
        /** How much worse a wrong correction is than a missed one: it has to be noticed and undone. */
        const val WRONG_PENALTY = 3f

        private const val SEED = 42
        private const val ACCENTS_SEED_OFFSET = 3
        private const val TWO_EDIT_SHARE = 0.2
        private const val SYNTHETIC_SOURCE_WORDS = 5000
        private const val SLIP_KINDS = 5
        private const val MIN_COMPLETED_LENGTH = 4
        private const val SLOW_LATENCY_QUERIES = 200
        private const val NANOS_PER_MICRO = 1000f
        private const val P95 = 0.95
        private const val JW_BASELINE_FREQ_WEIGHT = 0.05f
        private val COMBINING_MARKS = Regex("\\p{Mn}+")
        private val LIGATURES = mapOf('ß' to "ss", 'ẞ' to "SS", 'æ' to "ae", 'Æ' to "AE", 'œ' to "oe", 'Œ' to "OE")

        private val cached = HashMap<String, Evaluation>()

        /** Returns the evaluation of [language], made once. */
        @Synchronized
        fun of(language: LanguageCase) = cached.getOrPut(language.locale) { Evaluation(language) }

        /** Returns [word] as typed without accents: without combining marks, and with ß, æ and œ spelled out. */
        fun stripAccents(word: String): String {
            val stripped = Normalizer.normalize(word, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
            return buildString { stripped.forEach { c -> append(LIGATURES[c] ?: c.toString()) } }
        }

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

        /** Dev and test halves, split by intended word so the same word never lands in both. */
        fun split(cases: List<TypoCase>, dev: Boolean) = cases.filter { (it.intended.hashCode() and 1 == 0) == dev }

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
            margin: Float = engine.rules.weights.margin,
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
            margin: Float = engine.rules.weights.margin,
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
    }
}

/** Every edit costs 1, the textbook optimal string alignment distance. */
object UniformErrorModel : ErrorModel {
    override val transposition = 1f
    override val firstLetterFactor = 1f

    override fun substitution(typed: IntArray, i: Int, intended: Int) = 1f

    override fun insertion(typed: IntArray, i: Int) = 1f

    override fun deletion(intended: Int, previous: Int) = 1f
}
