package org.fossify.keyboard.suggestions

import kotlin.math.hypot

/**
 * Key centres of the active layout in key-width units, and the substitution costs derived from them. Symbols without a
 * key (such as the apostrophe on most layouts) are as far from everything as possible.
 */
class KeyboardGeometry private constructor(private val xs: FloatArray, private val ys: FloatArray) {
    private val substitutionCosts = FloatArray(Alphabet.size * Alphabet.size)
    private val neighbours = BooleanArray(Alphabet.size * Alphabet.size)

    init {
        for (a in 0 until Alphabet.size) {
            for (b in 0 until Alphabet.size) {
                val distance = distance(a, b)
                val index = a * Alphabet.size + b
                substitutionCosts[index] = when {
                    a == b -> 0f
                    distance.isNaN() -> EngineConstants.SUB_MAX
                    else -> minOf(
                        EngineConstants.SUB_MAX,
                        EngineConstants.SUB_BASE + EngineConstants.SUB_PER_KEY * distance
                    )
                }
                neighbours[index] = a != b && distance <= EngineConstants.NEIGHBOUR_DISTANCE
            }
        }
    }

    /** Returns the distance between the key centres of two symbols, or NaN if one of them has no key. */
    fun distance(a: Int, b: Int) = hypot(xs[a] - xs[b], ys[a] - ys[b])

    fun substitutionCost(typed: Int, intended: Int) = substitutionCosts[typed * Alphabet.size + intended]

    fun areNeighbours(a: Int, b: Int) = neighbours[a * Alphabet.size + b]

    /** The char of a key and its bounds, in any unit. */
    class KeyBounds(val char: Char, val x: Int, val y: Int, val width: Int, val height: Int)

    companion object {
        private const val HALF = 0.5f
        private const val MIN_LETTER_KEYS = 20

        /** Builds the geometry from key centres in any unit, given the width of a key in that unit. */
        fun fromKeys(chars: CharArray, xs: FloatArray, ys: FloatArray, keyWidth: Float): KeyboardGeometry {
            val symbolXs = FloatArray(Alphabet.size) { Float.NaN }
            val symbolYs = FloatArray(Alphabet.size) { Float.NaN }
            for (i in chars.indices) {
                val symbol = Alphabet.symbolOf(Alphabet.fold(chars[i]))
                if (symbol != Alphabet.NO_SYMBOL && symbolXs[symbol].isNaN()) {
                    symbolXs[symbol] = xs[i] / keyWidth
                    symbolYs[symbol] = ys[i] / keyWidth
                }
            }

            return KeyboardGeometry(symbolXs, symbolYs)
        }

        /**
         * Builds the geometry from the bounds of the keys of a layout, or returns null if it has fewer than
         * [MIN_LETTER_KEYS] letters of the alphabet. x is measured in median key widths and y in median key heights,
         * so rows are one unit apart whatever the key height, like in the grid the costs were tuned on.
         */
        fun fromKeyBounds(keys: List<KeyBounds>): KeyboardGeometry? {
            val alphabetKeys = keys.filter { Alphabet.symbolOf(Alphabet.fold(it.char)) != Alphabet.NO_SYMBOL }
            if (alphabetKeys.count { it.char.isLetter() } < MIN_LETTER_KEYS) return null

            val keyWidth = median(alphabetKeys.map { it.width })
            val keyHeight = median(alphabetKeys.map { it.height })
            if (keyWidth <= 0 || keyHeight <= 0) return null

            return fromKeys(
                chars = CharArray(alphabetKeys.size) { alphabetKeys[it].char },
                xs = FloatArray(alphabetKeys.size) { alphabetKeys[it].run { (x + width * HALF) / keyWidth } },
                ys = FloatArray(alphabetKeys.size) { alphabetKeys[it].run { (y + height * HALF) / keyHeight } },
                keyWidth = 1f
            )
        }

        private fun median(values: List<Int>) = values.sorted()[values.size / 2]

        /** Builds the geometry of a grid of equally wide keys, each row shifted right by the given number of keys. */
        fun fromRows(rows: List<String>, offsets: List<Float>): KeyboardGeometry {
            val chars = StringBuilder()
            val xs = ArrayList<Float>()
            val ys = ArrayList<Float>()
            rows.forEachIndexed { row, keys ->
                keys.forEachIndexed { column, char ->
                    chars.append(char)
                    xs.add(offsets[row] + column + HALF)
                    ys.add(row + HALF)
                }
            }

            return fromKeys(chars.toString().toCharArray(), xs.toFloatArray(), ys.toFloatArray(), 1f)
        }

        val QWERTY = fromRows(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), listOf(0f, HALF, 1f + HALF))
    }
}
