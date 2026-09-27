package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * The weights study behind [EngineConstants]. It only runs when asked, as it takes a few minutes:
 * `TUNE_ENGINE=1 ./gradlew testFossDebugUnitTest --tests '*EngineTuningTest*'`
 */
class EngineTuningTest {

    @Test
    fun tunableModelMatchesTheEngine() {
        val engine = KeyboardErrorModel(KeyboardGeometry.QWERTY)
        val tunable = TunableErrorModel(KeyboardGeometry.QWERTY, Weights.CURRENT)
        assertEquals(engine.transposition, tunable.transposition)
        assertEquals(engine.firstLetterFactor, tunable.firstLetterFactor)
        for (a in 0 until Alphabet.size) {
            for (b in 0 until Alphabet.size) {
                val typed = intArrayOf(a, b)
                assertEquals(engine.substitution(typed, 0, b), tunable.substitution(typed, 0, b), DELTA)
                assertEquals(engine.insertion(typed, 0), tunable.insertion(typed, 0), DELTA)
                assertEquals(engine.insertion(typed, 1), tunable.insertion(typed, 1), DELTA)
                assertEquals(engine.deletion(a, b), tunable.deletion(a, b), DELTA)
            }
        }
    }

    @Test
    fun study() {
        assumeTrue("Set TUNE_ENGINE=1 to run the weights study", System.getenv("TUNE_ENGINE") != null)
        val dev = EngineTuning(dev = true)
        val trials = dev.study()
        val baseline = trials.first()
        val feasible = trials.filter { it.isFeasible(baseline, EngineTuning.TOLERANCE) }
            .sortedByDescending { it.objective }

        println("TUNING ${trials.size} trials, ${feasible.size} feasible. Best on the dev half:")
        println("TUNING | Dev objective | Weights |")
        (listOf(baseline) + feasible.take(TOP_TRIALS)).forEach {
            println("TUNING | %.2f | ${it.weights} |".format(it.objective))
        }

        // Round the best weights, then sweep the margin again on its 0.1 grid
        val rounded = Weights.of(
            feasible.first().weights.values().map { (it * ROUNDING).roundToInt() / ROUNDING }.toFloatArray()
        )
        val sweep = (0..MARGIN_STEPS).map { rounded.copy(margin = it / MARGIN_STEPS.toFloat()) }
            .parallelStream().map { dev.measure(it) }.toList()
        sweep.forEach { println("TUNING margin %.1f: dev objective %.2f".format(it.weights.margin, it.objective)) }
        val best = sweep.filter { it.isFeasible(baseline, EngineTuning.TOLERANCE) }
            .maxWith(compareBy<Trial> { it.objective }.thenBy { it.weights.margin })

        val test = EngineTuning(dev = false)
        val rows = listOf(
            "Current, dev" to baseline,
            "Tuned, dev" to best,
            "Current, test" to test.measure(Weights.CURRENT),
            "Tuned, test" to test.measure(best.weights),
        )

        println("TUNING Tuned: ${best.weights}")
        println(
            "TUNING | | Objective | Fixed syn/real/mobile | Wrong syn/real/mobile | Unknown changed " +
                "| Top-3 syn/real/mobile | Completion top-3 | Valid changed |"
        )
        rows.forEach { (name, trial) ->
            val autocorrect = trial.autocorrect
            val suggestions = trial.suggestions
            println(
                "TUNING | $name | %.2f | %s | %s | %.1f | %s | %.1f | %.1f |".format(
                    trial.objective, autocorrect.fixed.format(), autocorrect.wrong.format(), autocorrect.unknownChanged,
                    suggestions.top3.format(), suggestions.completionTop3, suggestions.validChanged
                )
            )
        }
    }

    private fun List<Float>.format() = joinToString(" / ") { "%.1f".format(it) }

    private companion object {
        const val DELTA = 1e-5f
        const val TOP_TRIALS = 10
        const val ROUNDING = 100f
        const val MARGIN_STEPS = 10
    }
}
