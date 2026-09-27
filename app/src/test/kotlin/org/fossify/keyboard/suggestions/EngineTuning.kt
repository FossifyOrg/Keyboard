package org.fossify.keyboard.suggestions

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/** The [EngineWeights] [EngineTuning] tunes, in their order in [values], with the range each is searched in. */
object WeightSpace {
    val NAMES = listOf(
        "SUB_BASE", "SUB_PER_KEY", "INS_NEAR", "DEL_APOSTROPHE", "DEL_DOUBLE", "TRANSPOSITION",
        "FIRST_LETTER_FACTOR", "COST_WEIGHT", "JW_WEIGHT", "AUTOCORRECT_MARGIN"
    )

    val RANGES = listOf(
        0.1f..0.8f, 0.1f..0.6f, 0.4f..1.0f, 0.02f..0.5f, 0.3f..1.0f, 0.3f..1.0f, 1.0f..1.6f, 2f..6f, 2f..10f,
        0f..1.2f
    )

    fun values(weights: EngineWeights) = with(weights) {
        floatArrayOf(
            subBase, subPerKey, insNear, delApostrophe, delDouble, transposition, firstLetterFactor, costWeight,
            jwWeight, margin
        )
    }

    fun of(values: FloatArray) = EngineWeights(
        subBase = values[0], subPerKey = values[1], insNear = values[2], delApostrophe = values[3],
        delDouble = values[4], transposition = values[5], firstLetterFactor = values[6], costWeight = values[7],
        jwWeight = values[8], margin = values[9]
    )

    fun describe(weights: EngineWeights) =
        NAMES.zip(values(weights).toList()).joinToString { (name, value) -> "$name=%.2f".format(value) }

    /** The weights as Kotlin, to paste into [LanguageRules]. */
    fun kotlin(weights: EngineWeights): String {
        val names = listOf(
            "subBase", "subPerKey", "insNear", "delApostrophe", "delDouble", "transposition", "firstLetterFactor",
            "costWeight", "jwWeight", "margin"
        )
        return names.zip(values(weights).toList())
            .joinToString(prefix = "EngineWeights(", postfix = ")") { (name, value) -> "$name = %.2ff".format(value) }
    }
}

/** Autocorrect on each typo dataset, in percent: typos fixed, typos changed into a wrong word, and unknown words. */
class AutocorrectRates(val fixed: List<Float>, val wrong: List<Float>, val unknownChanged: Float) {
    /** Share of typos fixed minus [Evaluation.WRONG_PENALTY] times the share changed into a wrong word, on average. */
    val objective: Float
        get() {
            val typos = fixed.indices.sumOf { (fixed[it] - Evaluation.WRONG_PENALTY * wrong[it]).toDouble() }
            return ((typos - Evaluation.WRONG_PENALTY * unknownChanged) / (fixed.size + 1)).toFloat()
        }

    /** Whether no dataset gets worse than with [baseline] by more than [tolerance] points. */
    fun isKept(baseline: AutocorrectRates, tolerance: Float): Boolean {
        val typosKept = fixed.indices.all {
            fixed[it] >= baseline.fixed[it] - tolerance && wrong[it] <= baseline.wrong[it] + tolerance
        }
        return typosKept && unknownChanged <= baseline.unknownChanged + tolerance
    }
}

/** Suggestions on each typo dataset, in percent: intended word in the top 3, completions, and valid words replaced. */
class SuggestionRates(val top3: List<Float>, val completionTop3: Float, val validChanged: Float)

/** How a set of weights does on one half of the datasets, and how many everyday corrections it loses. */
class Trial(
    val weights: EngineWeights,
    val autocorrect: AutocorrectRates,
    val suggestions: SuggestionRates,
    val violations: Int,
) {
    val objective: Float
        get() = autocorrect.objective

    /**
     * A trial only counts if no dataset gets worse than with [baseline] by more than [tolerance] points, in fixed or
     * wrong autocorrections, top-3 suggestions or completions, valid words are never changed, and every everyday
     * correction still works.
     */
    fun isFeasible(baseline: Trial, tolerance: Float): Boolean {
        val baseSuggestions = baseline.suggestions
        val suggestionsKept = with(suggestions) {
            top3.indices.all { top3[it] >= baseSuggestions.top3[it] - tolerance } &&
                completionTop3 >= baseSuggestions.completionTop3 - tolerance &&
                validChanged <= baseSuggestions.validChanged
        }
        return violations == 0 && autocorrect.isKept(baseline.autocorrect, tolerance) && suggestionsKept
    }
}

/**
 * Tunes the [EngineWeights] of a [language] like an Optuna study: random trials over [WeightSpace.RANGES], then
 * generations sampled around the best feasible trials with a shrinking spread. Everything is seeded, so a study always
 * gives the same result. Trials are measured on the [dev] or the test half of the language's datasets.
 */
class EngineTuning(val language: LanguageCase, dev: Boolean) {
    private val evaluation = language.evaluation
    private val judge = language.engine()

    /** The typo datasets, by name: synthetic slips, real misspellings, mobile slips, and missing accents if any. */
    val typos: Map<String, List<TypoCase>> = buildMap {
        put("Synthetic", Evaluation.split(evaluation.syntheticCases(SYNTHETIC_CASES), dev))
        put("Real", Evaluation.split(evaluation.usable(evaluation.cases("real_misspellings.tsv")), dev))
        put("Mobile", Evaluation.split(evaluation.usable(evaluation.cases("mobile_typos.tsv")), dev))
        if (language.rules.restoresAccents) {
            put("Accents", Evaluation.split(evaluation.accentCases(ACCENT_CASES), dev))
        }
    }

    val unknownWords = evaluation.words("oov_words.txt").filter { !judge.isWord(it) }

    /** Valid words spelled like others but for their accents, which must never be changed. */
    val ambiguousWords = evaluation.words("ambiguous_words.txt")

    private val completions = Evaluation.split(evaluation.completionCases(COMPLETION_CASES), dev)
    private val validWords = evaluation.validWords(VALID_WORDS)

    /** Autocorrect with [weights] on each typo dataset and the unknown words. */
    fun autocorrectRates(weights: EngineWeights): AutocorrectRates {
        val engine = language.engine(weights)
        val rates = typos.values.map { rates(engine, weights, it) }
        return autocorrectRates(engine, weights, rates)
    }

    fun measure(weights: EngineWeights): Trial {
        val engine = language.engine(weights)
        val rates = typos.values.map { rates(engine, weights, it) }
        val completed = completions.count { case ->
            engine.suggest(case.typed).words.any { it.equals(case.intended, ignoreCase = true) }
        }
        val validChanged = validWords.count { word ->
            engine.suggest(word).words.firstOrNull()?.equals(word, ignoreCase = true) != true
        }
        val lost = language.mustCorrect.count { (typed, word) -> correct(engine, typed, weights) != word } +
            language.mustSuggest.count { (typed, word) -> engine.suggest(typed).words.firstOrNull() != word } +
            ambiguousWords.count { word -> correct(engine, word, weights)?.equals(word, ignoreCase = true) == false }

        return Trial(
            weights = weights,
            autocorrect = autocorrectRates(engine, weights, rates),
            suggestions = SuggestionRates(
                top3 = rates.map { it.top3 },
                completionTop3 = percent(completed, completions.size),
                validChanged = percent(validChanged, validWords.size),
            ),
            violations = lost,
        )
    }

    /** Autocorrect and top-3 suggestions on one typo dataset, in percent. */
    private class DatasetRates(val fixed: Float, val wrong: Float, val top3: Float)

    private fun rates(engine: SuggestionEngine, weights: EngineWeights, cases: List<TypoCase>): DatasetRates {
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

        return DatasetRates(percent(right, cases.size), percent(changed, cases.size), percent(inTop3, cases.size))
    }

    private fun autocorrectRates(
        engine: SuggestionEngine,
        weights: EngineWeights,
        rates: List<DatasetRates>,
    ): AutocorrectRates {
        val unknownChanged = unknownWords.count { word ->
            correct(word, engine.suggest(word), weights)?.equals(word, ignoreCase = true) == false
        }
        val unknownRate = percent(unknownChanged, unknownWords.size)
        return AutocorrectRates(rates.map { it.fixed }, rates.map { it.wrong }, unknownRate)
    }

    /** Returns the everyday corrections [weights] lose, and the ambiguous words they change. */
    fun lost(weights: EngineWeights): List<String> {
        val engine = language.engine(weights)
        return language.mustCorrect.filter { (typed, word) -> correct(engine, typed, weights) != word }
            .map { (typed, word) -> "$typed → $word corrected to ${correct(engine, typed, weights)}" } +
            language.mustSuggest.filter { (typed, word) -> engine.suggest(typed).words.firstOrNull() != word }
                .map { (typed, word) -> "$typed → $word suggested as ${engine.suggest(typed).words}" } +
            ambiguousWords.filter { word -> correct(engine, word, weights)?.equals(word, ignoreCase = true) == false }
                .map { word -> "$word changed to ${correct(engine, word, weights)}" }
    }

    /** Runs the study from the language's current weights and returns every trial, the first being those. */
    fun study(random: Random = Random(SEED)): List<Trial> {
        val trials = ArrayList<Trial>()
        trials.add(measure(language.rules.weights))
        val baseline = trials.first()

        trials.addAll(measureAll(List(RANDOM_TRIALS) { sample(random) }))
        var spread = INITIAL_SPREAD
        repeat(GENERATIONS) {
            // Without feasible trials yet, the next generation is sampled around the current weights
            val parents = trials.filter { it.isFeasible(baseline, TOLERANCE) }
                .sortedByDescending { it.objective }
                .take(PARENTS)
                .ifEmpty { listOf(baseline) }
            val children = List(GENERATION_SIZE) { mutate(parents[it % parents.size].weights, spread, random) }
            trials.addAll(measureAll(children))
            spread *= SPREAD_DECAY
        }

        return trials
    }

    private fun measureAll(batch: List<EngineWeights>) = batch.parallelStream().map { measure(it) }.toList()

    private fun sample(random: Random): EngineWeights {
        return WeightSpace.of(FloatArray(WeightSpace.RANGES.size) { WeightSpace.RANGES[it].sample(random) })
    }

    private fun mutate(parent: EngineWeights, spread: Float, random: Random): EngineWeights {
        val values = WeightSpace.values(parent)
        return WeightSpace.of(FloatArray(values.size) {
            val range = WeightSpace.RANGES[it]
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

    private fun percent(count: Int, total: Int) = PERCENT * count / total

    companion object {
        /** Top-3 suggestions and completions may get worse by this many points at most. */
        const val TOLERANCE = 1f

        private const val PERCENT = 100f
        private const val SEED = 7
        private const val SYNTHETIC_CASES = 2000
        private const val ACCENT_CASES = 1000
        private const val COMPLETION_CASES = 2000
        private const val VALID_WORDS = 2000
        private const val RANDOM_TRIALS = 96
        private const val GENERATIONS = 4
        private const val GENERATION_SIZE = 48
        private const val PARENTS = 8
        private const val INITIAL_SPREAD = 0.15f
        private const val SPREAD_DECAY = 0.6f

        fun correct(engine: SuggestionEngine, typed: String, weights: EngineWeights) =
            correct(typed, engine.suggest(typed), weights)

        fun correct(typed: String, suggestions: Suggestions, weights: EngineWeights): String? {
            return AutocorrectPolicy.correctionFor(typed, suggestions, emptySet(), false, weights.margin)?.word
        }
    }
}
