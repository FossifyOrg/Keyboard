package org.fossify.keyboard.suggestions

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs

/**
 * Compares the suggestion engine with simpler approaches on the test half of the typo datasets of each language and
 * prints a table. The [EngineWeights] of a language were tuned on the dev half; the autocorrect margin is swept there
 * by [autocorrectMarginIsTuned]. The slow full-scan baselines are only compared on English.
 */
@RunWith(Parameterized::class)
class TypoEvaluationTest(locale: String) {
    private val language = LanguageCase.of(locale)
    private val evaluation = language.evaluation
    private val test = EngineTuning(language, dev = false)
    private val typos = test.typos
    private val completions = Evaluation.split(evaluation.completionCases(COMPLETION_CASES), dev = false)
    private val validWords = evaluation.validWords(VALID_WORDS)
    private val unknownWords = test.unknownWords
    private val weights = language.rules.weights
    private val hasBaselines = language == LanguageCase.ENGLISH

    private val systems = linkedMapOf<String, SuggestionSystem>().apply {
        if (hasBaselines) {
            put(LEVENSHTEIN, evaluation.levenshteinBaseline)
            put(JARO_WINKLER, evaluation.jaroWinklerBaseline)
        }
        put("OSA trie, unweighted", evaluation.engineSystem(UniformErrorModel, Ranker(weights.costWeight, 0f)))
        put("Keyboard OSA", evaluation.engineSystem(ranker = Ranker(weights.costWeight, 0f)))
        put(KEYBOARD_JW, evaluation.engineSystem())
    }

    @Test
    fun keyboardAwareSearchBeatsBaselines() {
        val results = systems.mapValues { (_, system) ->
            Row(
                typos = typos.mapValues { (_, cases) -> Evaluation.evaluate(system, cases) },
                completion = Evaluation.evaluate(system, completions),
                falseCorrections = Evaluation.falseCorrectionRate(system, validWords),
                latency = Evaluation.latency(system, typos.values.flatten().map { it.typed }),
            )
        }

        printTable(results)

        val best = results.getValue(KEYBOARD_JW)
        if (hasBaselines) {
            for (baseline in listOf(LEVENSHTEIN, JARO_WINKLER).map { results.getValue(it) }) {
                assertTrue(best.typos.getValue(SYNTHETIC).top1 > baseline.typos.getValue(SYNTHETIC).top1)
                assertTrue(best.typos.getValue(REAL).top1 >= baseline.typos.getValue(REAL).top1)
                assertTrue(best.typos.getValue(MOBILE).top1 > baseline.typos.getValue(MOBILE).top1)
            }
        }

        assertTrue(best.completion.top3 >= MIN_COMPLETION_TOP3)

        for ((name, result) in best.typos) {
            assertTrue("$name top-1 ${result.top1}", result.top1 >= MIN_TOP1)
        }
        assertTrue(best.falseCorrections <= MAX_FALSE_CORRECTIONS)
        assertTrue(best.latency.second < MAX_P95_MICROS)
    }

    @Test
    fun autocorrectIsConservative() {
        val engine = language.engine()
        val results = typos.mapValues { (_, cases) -> Evaluation.autocorrect(engine, cases) }
        val changedUnknownWords = Evaluation.autocorrectedWords(engine, unknownWords)
        val changedValidWords = Evaluation.autocorrectedWords(engine, validWords)
        val changedAmbiguousWords = test.ambiguousWords.filter { Evaluation.autocorrectedWords(engine, listOf(it)) > 0 }

        println("| Autocorrect | Right | Wrong |")
        println("|---|---|---|")
        for ((name, result) in results) {
            println("| $name | %.1f%% | %.1f%% |".format(result.right * 100, result.wrong * 100))
        }
        println("| Unknown words changed (n=${unknownWords.size}) | | %.1f%% |".format(changedUnknownWords * 100))
        println("| Valid words changed (n=${validWords.size}) | | %.1f%% |".format(changedValidWords * 100))
        if (test.ambiguousWords.isNotEmpty()) {
            println("| Ambiguous words changed (n=${test.ambiguousWords.size}) | | $changedAmbiguousWords |")
        }

        for ((name, result) in results) {
            assertTrue("$name right ${result.right}", result.right >= MIN_AUTOCORRECT_RIGHT)
            assertTrue("$name wrong ${result.wrong}", result.wrong <= language.maxAutocorrectWrong)
        }

        assertTrue("unknown words changed $changedUnknownWords", changedUnknownWords <= language.maxUnknownChanged)
        assertTrue(changedValidWords == 0f)
        assertTrue(changedAmbiguousWords.toString(), changedAmbiguousWords.isEmpty())
    }

    /**
     * Sweeps the autocorrect margin from 0 to 1 on the dev half and checks that the margin of the language scores
     * best. A margin scores the share of typos it fixes minus [Evaluation.WRONG_PENALTY] times the share it changes
     * into a wrong word, averaged over the typo datasets and the unknown words. Like in the weights study, margins
     * that make a dataset worse by more than [EngineTuning.TOLERANCE] points don't count.
     */
    @Test
    fun autocorrectMarginIsTuned() {
        val engine = language.engine()
        val dev = EngineTuning(language, dev = true)
        val datasets = dev.typos.values.toList()
        val sweep = (0..MARGIN_STEPS).map { step ->
            val margin = step / MARGIN_STEPS.toFloat()
            val results = datasets.map { cases -> Evaluation.autocorrect(engine, cases, margin) }
            val fixes = results.sumOf { (it.right - Evaluation.WRONG_PENALTY * it.wrong).toDouble() }
            val changedUnknownWords = Evaluation.autocorrectedWords(engine, dev.unknownWords, margin)
            val score = (fixes - Evaluation.WRONG_PENALTY * changedUnknownWords) / (datasets.size + 1)
            MarginResult(margin, results, changedUnknownWords, score)
        }

        println("| Autocorrect margin | Dev score (fixed - ${Evaluation.WRONG_PENALTY} × wrong) |")
        println("|---|---|")
        sweep.forEach { println("| %.1f | %.2f |".format(it.margin, it.score * 100)) }

        val configured = sweep.first { abs(it.margin - weights.margin) < MARGIN_EPSILON }
        val best = sweep.filter { it.keeps(configured, EngineTuning.TOLERANCE / 100) }.maxOf { it.score }
        assertTrue(configured.score >= best - MARGIN_EPSILON)
    }

    private class MarginResult(
        val margin: Float,
        val results: List<AutocorrectResult>,
        val changedUnknownWords: Float,
        val score: Double,
    ) {
        fun keeps(baseline: MarginResult, tolerance: Float): Boolean {
            val typosKept = results.indices.all {
                results[it].right >= baseline.results[it].right - tolerance &&
                    results[it].wrong <= baseline.results[it].wrong + tolerance
            }
            return typosKept && changedUnknownWords <= baseline.changedUnknownWords + tolerance
        }
    }

    private class Row(
        val typos: Map<String, EvaluationResult>,
        val completion: EvaluationResult,
        val falseCorrections: Float,
        val latency: Pair<Float, Float>,
    )

    private fun printTable(results: Map<String, Row>) {
        val datasets = typos.entries.joinToString(" ") { (name, cases) -> "| $name top-1 / top-3 (n=${cases.size})" }
        println(
            "| System $datasets | Completion top-3 (n=${completions.size}) " +
                "| False corrections (n=${validWords.size}) | p50 / p95 µs |"
        )
        println("|---" + "|---".repeat(typos.size) + "|---|---|---|")
        for ((name, row) in results) {
            val cells = row.typos.values.joinToString(" | ") { percent(it) }
            println(
                "| $name | $cells " +
                    "| %.1f%% | %.1f%% | %.0f / %.0f |".format(
                        row.completion.top3 * 100, row.falseCorrections * 100, row.latency.first, row.latency.second)
            )
        }
    }

    private fun percent(result: EvaluationResult) = "%.1f%% / %.1f%%".format(result.top1 * 100, result.top3 * 100)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun locales() = LanguageCase.ALL.map { it.locale }

        private const val LEVENSHTEIN = "Levenshtein full scan (#346)"
        private const val JARO_WINKLER = "Jaro-Winkler + frequency"
        private const val KEYBOARD_JW = "Keyboard OSA + Jaro-Winkler"
        private const val SYNTHETIC = "Synthetic"
        private const val REAL = "Real"
        private const val MOBILE = "Mobile"
        private const val COMPLETION_CASES = 2000
        private const val VALID_WORDS = 2000

        private const val MIN_TOP1 = 0.5f
        private const val MIN_COMPLETION_TOP3 = 0.5f
        private const val MAX_FALSE_CORRECTIONS = 0.05f
        private const val MAX_P95_MICROS = 10_000f

        private const val MARGIN_STEPS = 10
        private const val MARGIN_EPSILON = 1e-4f

        private const val MIN_AUTOCORRECT_RIGHT = 0.35f
    }
}
