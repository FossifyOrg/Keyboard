package org.fossify.keyboard.suggestions

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/** The error model and ranking weights [EngineTuning] tunes. Costs are in edits, like in [EngineConstants]. */
data class Weights(
    val subBase: Float,
    val subPerKey: Float,
    val insNear: Float,
    val delApostrophe: Float,
    val delDouble: Float,
    val transposition: Float,
    val firstLetterFactor: Float,
    val costWeight: Float,
    val jwWeight: Float,
    val margin: Float,
) {
    fun values() = floatArrayOf(
        subBase, subPerKey, insNear, delApostrophe, delDouble, transposition, firstLetterFactor, costWeight, jwWeight,
        margin
    )

    override fun toString() = NAMES.zip(values().toList()).joinToString { (name, value) -> "$name=%.2f".format(value) }

    companion object {
        val NAMES = listOf(
            "SUB_BASE", "SUB_PER_KEY", "INS_NEAR", "DEL_APOSTROPHE", "DEL_DOUBLE", "TRANSPOSITION",
            "FIRST_LETTER_FACTOR", "COST_WEIGHT", "JW_WEIGHT", "AUTOCORRECT_MARGIN"
        )

        /** The range each weight is searched in. */
        val RANGES = listOf(
            0.1f..0.8f, 0.1f..0.6f, 0.4f..1.0f, 0.02f..0.5f, 0.3f..1.0f, 0.3f..1.0f, 1.0f..1.6f, 2f..6f, 2f..10f,
            0f..1.2f
        )

        val CURRENT = Weights(
            EngineConstants.SUB_BASE, EngineConstants.SUB_PER_KEY, EngineConstants.INS_NEAR,
            EngineConstants.DEL_APOSTROPHE, EngineConstants.DEL_DOUBLE, EngineConstants.TRANSPOSITION,
            EngineConstants.FIRST_LETTER_FACTOR, EngineConstants.COST_WEIGHT, EngineConstants.JW_WEIGHT,
            EngineConstants.AUTOCORRECT_MARGIN
        )

        fun of(values: FloatArray) = Weights(
            values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8], values[9]
        )
    }
}

/** [KeyboardErrorModel] with the given [weights] instead of those in [EngineConstants]. */
class TunableErrorModel(private val geometry: KeyboardGeometry, private val weights: Weights) : ErrorModel {
    private val apostrophe = Alphabet.symbolOf(Alphabet.APOSTROPHE)

    override val transposition = weights.transposition

    override val firstLetterFactor = weights.firstLetterFactor

    override fun substitution(typed: IntArray, i: Int, intended: Int): Float {
        val distance = geometry.distance(typed[i], intended)
        return when {
            typed[i] == intended -> 0f
            distance.isNaN() -> EngineConstants.SUB_MAX
            else -> min(EngineConstants.SUB_MAX, weights.subBase + weights.subPerKey * distance)
        }
    }

    override fun insertion(typed: IntArray, i: Int): Float {
        val symbol = typed[i]
        val nearPrevious = i > 0 && (typed[i - 1] == symbol || geometry.areNeighbours(typed[i - 1], symbol))
        val nearNext = i + 1 < typed.size && geometry.areNeighbours(typed[i + 1], symbol)
        return if (nearPrevious || nearNext) weights.insNear else EngineConstants.INS_DEFAULT
    }

    override fun deletion(intended: Int, previous: Int): Float {
        return when (intended) {
            apostrophe -> weights.delApostrophe
            previous -> weights.delDouble
            else -> EngineConstants.DEL_DEFAULT
        }
    }
}

/** Autocorrect on each typo dataset, in percent: typos fixed, typos changed into a wrong word, and unknown words. */
class AutocorrectRates(val fixed: List<Float>, val wrong: List<Float>, val unknownChanged: Float)

/** Suggestions on each typo dataset, in percent: intended word in the top 3, completions, and valid words replaced. */
class SuggestionRates(val top3: List<Float>, val completionTop3: Float, val validChanged: Float)

/** How a set of weights does on one half of the datasets, and how many everyday corrections it loses. */
class Trial(
    val weights: Weights,
    val autocorrect: AutocorrectRates,
    val suggestions: SuggestionRates,
    val violations: Int,
) {
    /** Share of typos fixed minus [Evaluation.WRONG_PENALTY] times the share changed into a wrong word, on average. */
    val objective: Float
        get() = with(autocorrect) {
            val typos = fixed.indices.sumOf { (fixed[it] - Evaluation.WRONG_PENALTY * wrong[it]).toDouble() }
            ((typos - Evaluation.WRONG_PENALTY * unknownChanged) / (fixed.size + 1)).toFloat()
        }

    /**
     * A trial only counts if no dataset gets worse than with [baseline] by more than [tolerance] points, in fixed or
     * wrong autocorrections, top-3 suggestions or completions, valid words are never changed, and every everyday
     * correction still works.
     */
    fun isFeasible(baseline: Trial, tolerance: Float): Boolean {
        val base = baseline.autocorrect
        val autocorrectKept = with(autocorrect) {
            fixed.indices.all { fixed[it] >= base.fixed[it] - tolerance && wrong[it] <= base.wrong[it] + tolerance } &&
                unknownChanged <= base.unknownChanged + tolerance
        }
        val baseSuggestions = baseline.suggestions
        val suggestionsKept = with(suggestions) {
            top3.indices.all { top3[it] >= baseSuggestions.top3[it] - tolerance } &&
                completionTop3 >= baseSuggestions.completionTop3 - tolerance &&
                validChanged <= baseSuggestions.validChanged
        }
        return violations == 0 && autocorrectKept && suggestionsKept
    }
}

/**
 * Tunes [Weights] like an Optuna study: random trials over [Weights.RANGES], then generations sampled around the best
 * feasible trials with a shrinking spread. Everything is seeded, so a study always gives the same result.
 */
class EngineTuning(private val dev: Boolean) {
    private val typos = listOf(
        Evaluation.split(Evaluation.syntheticCases(SYNTHETIC_CASES), dev),
        Evaluation.split(Evaluation.usable(Evaluation.loadCases("/typos/real_misspellings.tsv")), dev),
        Evaluation.split(Evaluation.usable(Evaluation.loadCases("/typos/mobile_typos.tsv")), dev),
    )
    private val unknownWords = Evaluation.loadWords("/typos/oov_words.txt").filter { it !in TestDictionary.trie }
    private val completions = Evaluation.split(Evaluation.completionCases(COMPLETION_CASES), dev)
    private val validWords = Evaluation.validWords(VALID_WORDS)

    fun measure(weights: Weights): Trial {
        val engine = SuggestionEngine(TestDictionary.trie, ranker = Ranker(weights.costWeight, weights.jwWeight))
        engine.errorModel = TunableErrorModel(KeyboardGeometry.QWERTY, weights)
        val fixed = ArrayList<Float>()
        val wrong = ArrayList<Float>()
        val top3 = ArrayList<Float>()
        for (cases in typos) {
            var right = 0
            var changed = 0
            var inTop3 = 0
            for (case in cases) {
                val suggestions = engine.suggest(case.typed)
                if (suggestions.words.any { it.equals(case.intended, ignoreCase = true) }) inTop3++
                val correction = correct(case.typed, suggestions, weights)
                when {
                    correction == null -> {}
                    correction.equals(case.intended, ignoreCase = true) -> right++
                    else -> changed++
                }
            }

            fixed.add(percent(right, cases.size))
            wrong.add(percent(changed, cases.size))
            top3.add(percent(inTop3, cases.size))
        }

        val unknownChanged = unknownWords.count { word ->
            correct(word, engine.suggest(word), weights)?.equals(word, ignoreCase = true) == false
        }
        val completed = completions.count { case ->
            engine.suggest(case.typed).words.any { it.equals(case.intended, ignoreCase = true) }
        }
        val validChanged = validWords.count { word ->
            engine.suggest(word).words.firstOrNull()?.equals(word, ignoreCase = true) != true
        }
        val lost = MUST_CORRECT.count { (typed, word) -> correct(typed, engine.suggest(typed), weights) != word } +
            MUST_SUGGEST.count { (typed, word) -> engine.suggest(typed).words.firstOrNull() != word }

        return Trial(
            weights = weights,
            autocorrect = AutocorrectRates(fixed, wrong, percent(unknownChanged, unknownWords.size)),
            suggestions = SuggestionRates(
                top3 = top3,
                completionTop3 = percent(completed, completions.size),
                validChanged = percent(validChanged, validWords.size),
            ),
            violations = lost,
        )
    }

    /** Runs the study and returns every trial, the first being the current weights. */
    fun study(random: Random = Random(SEED)): List<Trial> {
        val trials = ArrayList<Trial>()
        trials.add(measure(Weights.CURRENT))
        val baseline = trials.first()

        trials.addAll(measureAll(List(RANDOM_TRIALS) { sample(random) }))
        var spread = INITIAL_SPREAD
        repeat(GENERATIONS) {
            val parents = trials.filter { it.isFeasible(baseline, TOLERANCE) }
                .sortedByDescending { it.objective }
                .take(PARENTS)
            val children = List(GENERATION_SIZE) { mutate(parents[it % parents.size].weights, spread, random) }
            trials.addAll(measureAll(children))
            spread *= SPREAD_DECAY
        }

        return trials
    }

    private fun measureAll(batch: List<Weights>) = batch.parallelStream().map { measure(it) }.toList()

    private fun sample(random: Random): Weights {
        return Weights.of(FloatArray(Weights.RANGES.size) { Weights.RANGES[it].sample(random) })
    }

    private fun mutate(parent: Weights, spread: Float, random: Random): Weights {
        val values = parent.values()
        return Weights.of(FloatArray(values.size) {
            val range = Weights.RANGES[it]
            val width = range.endInclusive - range.start
            (values[it] + random.nextGaussian() * spread * width).coerceIn(range.start, range.endInclusive)
        })
    }

    private fun ClosedFloatingPointRange<Float>.sample(random: Random) =
        start + random.nextFloat() * (endInclusive - start)

    private fun Random.nextGaussian(): Float {
        // Box-Muller
        val u = 1.0 - nextDouble()
        val v = nextDouble()
        return (sqrt(-2 * ln(u)) * cos(2 * PI * v)).toFloat()
    }

    private fun percent(count: Int, total: Int) = 100f * count / total

    companion object {
        /** Top-3 suggestions and completions may get worse by this many points at most. */
        const val TOLERANCE = 1f

        private const val SEED = 7
        private const val SYNTHETIC_CASES = 2000
        private const val COMPLETION_CASES = 2000
        private const val VALID_WORDS = 2000
        private const val RANDOM_TRIALS = 96
        private const val GENERATIONS = 4
        private const val GENERATION_SIZE = 48
        private const val PARENTS = 8
        private const val INITIAL_SPREAD = 0.15f
        private const val SPREAD_DECAY = 0.6f

        /** Everyday corrections, from the unit tests and testing on a phone, that tuning may not lose. */
        private val MUST_CORRECT = listOf(
            "teh" to "the", "recieve" to "receive", "becuase" to "because", "i" to "I", "monday" to "Monday",
            "dont" to "don't", "im" to "I'm", "wuick" to "quick", "broen" to "brown",
        )
        private val MUST_SUGGEST = listOf(
            "yhe" to "the", "wrod" to "word", "adress" to "address", "thier" to "their", "tomorow" to "tomorrow",
            "definately" to "definitely", "goign" to "going", "knwo" to "know", "hapy" to "happy", "cafe" to "café",
        )

        fun correct(typed: String, suggestions: Suggestions, weights: Weights): String? {
            return AutocorrectPolicy.correctionFor(typed, suggestions, emptySet(), false, weights.margin)?.word
        }
    }
}
