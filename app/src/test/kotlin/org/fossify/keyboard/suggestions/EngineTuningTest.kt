package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * The weights study behind the [EngineWeights] of each language. It only runs when asked, as it takes a few minutes
 * per language: `TUNE_ENGINE=1 ./gradlew testFossDebugUnitTest --tests '*EngineTuningTest*'`. `TUNE_LOCALE=de_DE`
 * (or a comma-separated list) limits it to some languages.
 */
class EngineTuningTest {

    @Test
    fun englishWeightsAreTheOnesTunedForEnglish() {
        val tuned = EngineWeights(
            subBase = 0.4f, subPerKey = 0.3f, insNear = 0.8f, delApostrophe = 0.1f, delDouble = 0.6f,
            transposition = 0.65f, firstLetterFactor = 1.15f, costWeight = 3.5f, jwWeight = 6.0f, margin = 0.6f,
        )
        assertEquals(tuned, EngineWeights.ENGLISH)
        assertEquals(EngineWeights.ENGLISH, LanguageRules.forLocale("en_US").weights)
    }

    @Test
    fun weightSpaceRoundTrips() {
        val weights = WeightSpace.of(FloatArray(WeightSpace.NAMES.size) { it / 10f })
        assertEquals(weights, WeightSpace.of(WeightSpace.values(weights)))
    }

    @Test
    fun study() {
        assumeTrue("Set TUNE_ENGINE=1 to run the weights study", System.getenv("TUNE_ENGINE") != null)
        val locales = System.getenv("TUNE_LOCALE")?.split(",")?.map { it.trim() }
        LanguageCase.ALL.filter { locales == null || it.locale in locales }.forEach { study(it) }
    }

    private fun study(language: LanguageCase) {
        val dev = EngineTuning(language, dev = true)
        val trials = dev.study()
        val baseline = trials.first()
        val feasible = trials.filter { it.isFeasible(baseline, EngineTuning.TOLERANCE) }
            .sortedByDescending { it.objective }

        val name = language.locale
        dev.lost(baseline.weights).forEach { println("TUNING $name current weights lose: $it") }
        println("TUNING $name: ${trials.size} trials, ${feasible.size} feasible. Best on the dev half:")
        println("TUNING $name | Dev objective | Weights |")
        (listOf(baseline) + feasible.take(TOP_TRIALS)).forEach {
            println("TUNING $name | %.2f | ${WeightSpace.describe(it.weights)} |".format(it.objective))
        }

        // Round the best weights, then sweep the margin again on its 0.1 grid
        val rounded = WeightSpace.of(
            WeightSpace.values(feasible.first().weights).map { (it * ROUNDING).roundToInt() / ROUNDING }.toFloatArray()
        )
        val sweep = (0..MARGIN_STEPS).map { rounded.copy(margin = it / MARGIN_STEPS.toFloat()) }
            .parallelStream().map { dev.measure(it) }.toList()
        sweep.forEach {
            println("TUNING $name margin %.1f: dev objective %.2f".format(it.weights.margin, it.objective))
        }
        val best = sweep.filter { it.isFeasible(baseline, EngineTuning.TOLERANCE) }
            .maxWith(compareBy<Trial> { it.objective }.thenBy { it.weights.margin })

        val test = EngineTuning(language, dev = false)
        val rows = listOf(
            "Current, dev" to baseline,
            "Tuned, dev" to best,
            "Current, test" to test.measure(language.rules.weights),
            "Tuned, test" to test.measure(best.weights),
        )

        val datasets = dev.typos.keys.joinToString("/")
        println("TUNING $name tuned: ${WeightSpace.describe(best.weights)}")
        println("TUNING $name as Kotlin: ${WeightSpace.kotlin(best.weights)}")
        println(
            "TUNING $name | | Objective | Fixed $datasets | Wrong $datasets | Unknown changed " +
                "| Top-3 $datasets | Completion top-3 | Valid changed | Lost |"
        )
        rows.forEach { (row, trial) ->
            val autocorrect = trial.autocorrect
            val suggestions = trial.suggestions
            println(
                "TUNING $name | $row | %.2f | %s | %s | %.1f | %s | %.1f | %.1f | %d |".format(
                    trial.objective, autocorrect.fixed.format(), autocorrect.wrong.format(), autocorrect.unknownChanged,
                    suggestions.top3.format(), suggestions.completionTop3, suggestions.validChanged, trial.violations
                )
            )
        }
    }

    private fun List<Float>.format() = joinToString(" / ") { "%.1f".format(it) }

    private companion object {
        const val TOP_TRIALS = 10
        const val ROUNDING = 100f
        const val MARGIN_STEPS = 10
    }
}
