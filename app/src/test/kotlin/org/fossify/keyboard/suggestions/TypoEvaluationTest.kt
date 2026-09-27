package org.fossify.keyboard.suggestions

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Compares the suggestion engine with simpler approaches on the test half of three typo datasets and prints a table.
 * Constants in [EngineConstants] were tuned on the dev half; the autocorrect margin is swept there by
 * [autocorrectMarginIsTuned].
 */
class TypoEvaluationTest {

    private val synthetic = Evaluation.split(Evaluation.syntheticCases(SYNTHETIC_CASES), dev = false)
    private val real = half("/typos/real_misspellings.tsv", dev = false)
    private val mobile = half("/typos/mobile_typos.tsv", dev = false)
    private val completions = Evaluation.split(Evaluation.completionCases(COMPLETION_CASES), dev = false)
    private val validWords = Evaluation.validWords(VALID_WORDS)
    private val unknownWords = Evaluation.loadWords("/typos/oov_words.txt").filter { it !in TestDictionary.trie }

    private val systems = linkedMapOf(
        "Levenshtein full scan (#346)" to Evaluation.levenshteinBaseline,
        "Jaro-Winkler + frequency" to Evaluation.jaroWinklerBaseline,
        "OSA trie, unweighted" to Evaluation.engineSystem(UniformErrorModel, Ranker(jaroWinklerWeight = 0f)),
        "Keyboard OSA" to Evaluation.engineSystem(ranker = Ranker(jaroWinklerWeight = 0f)),
        KEYBOARD_JW to Evaluation.engineSystem(),
    )

    @Test
    fun keyboardAwareSearchBeatsBaselines() {
        val results = systems.mapValues { (_, system) ->
            Row(
                synthetic = Evaluation.evaluate(system, synthetic),
                real = Evaluation.evaluate(system, real),
                mobile = Evaluation.evaluate(system, mobile),
                completion = Evaluation.evaluate(system, completions),
                falseCorrections = Evaluation.falseCorrectionRate(system, validWords),
                latency = Evaluation.latency(system, (synthetic + real + mobile).map { it.typed }),
            )
        }

        printTable(results)

        val best = results.getValue(KEYBOARD_JW)
        val baselines = listOf("Levenshtein full scan (#346)", "Jaro-Winkler + frequency").map { results.getValue(it) }
        for (baseline in baselines) {
            assertTrue(best.synthetic.top1 > baseline.synthetic.top1)
            assertTrue(best.real.top1 >= baseline.real.top1)
            assertTrue(best.mobile.top1 > baseline.mobile.top1)
        }

        assertTrue(best.completion.top3 >= MIN_COMPLETION_TOP3)

        assertTrue(best.synthetic.top1 >= MIN_SYNTHETIC_TOP1)
        assertTrue(best.real.top1 >= MIN_REAL_TOP1)
        assertTrue(best.mobile.top1 >= MIN_MOBILE_TOP1)
        assertTrue(best.falseCorrections <= MAX_FALSE_CORRECTIONS)
        assertTrue(best.latency.second < MAX_P95_MICROS)
    }

    @Test
    fun autocorrectIsConservative() {
        val engine = SuggestionEngine(TestDictionary.trie)
        val results = linkedMapOf(
            "Synthetic" to Evaluation.autocorrect(engine, synthetic),
            "Real" to Evaluation.autocorrect(engine, real),
            "Mobile" to Evaluation.autocorrect(engine, mobile),
        )
        val changedUnknownWords = Evaluation.autocorrectedWords(engine, unknownWords)
        val changedValidWords = Evaluation.autocorrectedWords(engine, validWords)

        println("| Autocorrect | Right | Wrong |")
        println("|---|---|---|")
        for ((name, result) in results) {
            println("| $name | %.1f%% | %.1f%% |".format(result.right * 100, result.wrong * 100))
        }
        println("| Unknown words changed (n=${unknownWords.size}) | | %.1f%% |".format(changedUnknownWords * 100))
        println("| Valid words changed (n=${validWords.size}) | | %.1f%% |".format(changedValidWords * 100))

        for (result in results.values) {
            assertTrue(result.right >= MIN_AUTOCORRECT_RIGHT)
            assertTrue(result.wrong <= MAX_AUTOCORRECT_WRONG)
        }

        assertTrue(changedUnknownWords <= MAX_UNKNOWN_WORDS_CHANGED)
        assertTrue(changedValidWords == 0f)
    }

    /**
     * Sweeps the autocorrect margin from 0 to 1 on the dev half and checks that [EngineConstants.AUTOCORRECT_MARGIN]
     * scores best. A margin scores the share of typos it fixes minus [Evaluation.WRONG_PENALTY] times the share it
     * changes into a wrong word, averaged over the typo datasets and the unknown words.
     */
    @Test
    fun autocorrectMarginIsTuned() {
        val engine = SuggestionEngine(TestDictionary.trie)
        val datasets = listOf(
            Evaluation.split(Evaluation.syntheticCases(SYNTHETIC_CASES), dev = true),
            half("/typos/real_misspellings.tsv", dev = true),
            half("/typos/mobile_typos.tsv", dev = true),
        )

        val scores = (0..MARGIN_STEPS).map { step ->
            val margin = step / MARGIN_STEPS.toFloat()
            val fixes = datasets.sumOf { cases ->
                val result = Evaluation.autocorrect(engine, cases, margin)
                (result.right - Evaluation.WRONG_PENALTY * result.wrong).toDouble()
            }
            val changedUnknownWords = Evaluation.autocorrectedWords(engine, unknownWords, margin)
            margin to (fixes - Evaluation.WRONG_PENALTY * changedUnknownWords) / (datasets.size + 1)
        }

        println("| Autocorrect margin | Dev score (fixed - ${Evaluation.WRONG_PENALTY} × wrong) |")
        println("|---|---|")
        scores.forEach { (margin, score) -> println("| %.1f | %.2f |".format(margin, score * 100)) }

        val best = scores.maxOf { it.second }
        val configured = scores.first { abs(it.first - EngineConstants.AUTOCORRECT_MARGIN) < MARGIN_EPSILON }.second
        assertTrue(configured >= best - MARGIN_EPSILON)
    }

    private class Row(
        val synthetic: EvaluationResult,
        val real: EvaluationResult,
        val mobile: EvaluationResult,
        val completion: EvaluationResult,
        val falseCorrections: Float,
        val latency: Pair<Float, Float>,
    )

    private fun printTable(results: Map<String, Row>) {
        println(
            "| System | Synthetic top-1 / top-3 (n=${synthetic.size}) | Real top-1 / top-3 (n=${real.size}) " +
                "| Mobile top-1 / top-3 (n=${mobile.size}) | Completion top-3 (n=${completions.size}) " +
                "| False corrections (n=${validWords.size}) | p50 / p95 µs |"
        )
        println("|---|---|---|---|---|---|---|")
        for ((name, row) in results) {
            println(
                "| $name | ${percent(row.synthetic)} | ${percent(row.real)} | ${percent(row.mobile)} " +
                    "| %.1f%% | %.1f%% | %.0f / %.0f |".format(
                        row.completion.top3 * 100, row.falseCorrections * 100, row.latency.first, row.latency.second)
            )
        }
    }

    private fun half(resource: String, dev: Boolean) =
        Evaluation.split(Evaluation.usable(Evaluation.loadCases(resource)), dev)

    private fun percent(result: EvaluationResult) = "%.1f%% / %.1f%%".format(result.top1 * 100, result.top3 * 100)

    private companion object {
        const val KEYBOARD_JW = "Keyboard OSA + Jaro-Winkler"
        const val SYNTHETIC_CASES = 2000
        const val COMPLETION_CASES = 2000
        const val VALID_WORDS = 2000

        const val MIN_SYNTHETIC_TOP1 = 0.5f
        const val MIN_REAL_TOP1 = 0.5f
        const val MIN_MOBILE_TOP1 = 0.5f
        const val MIN_COMPLETION_TOP3 = 0.5f
        const val MAX_FALSE_CORRECTIONS = 0.05f
        const val MAX_P95_MICROS = 10_000f

        const val MARGIN_STEPS = 10
        const val MARGIN_EPSILON = 1e-4f

        const val MIN_AUTOCORRECT_RIGHT = 0.35f
        const val MAX_AUTOCORRECT_WRONG = 0.05f
        const val MAX_UNKNOWN_WORDS_CHANGED = 0.1f
    }
}
