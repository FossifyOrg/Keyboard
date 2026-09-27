package org.fossify.keyboard.suggestions

/** Costs of the edits that turn an intended word into what was typed. Chars are alphabet symbols. */
interface ErrorModel {
    /** [typed]`[i]` was typed instead of [intended]. */
    fun substitution(typed: IntArray, i: Int, intended: Int): Float

    /** [typed]`[i]` was typed although it doesn't belong to the word. */
    fun insertion(typed: IntArray, i: Int): Float

    /** [intended] is missing from the typed word, [previous] is the intended char before it or [Alphabet.NO_SYMBOL]. */
    fun deletion(intended: Int, previous: Int): Float

    val transposition: Float

    /** Edits of the first char are multiplied by this. */
    val firstLetterFactor: Float

    fun maxCost(length: Int): Float {
        return when {
            length <= EngineConstants.SHORT_WORD_LENGTH -> EngineConstants.MAX_COST_SHORT
            length <= EngineConstants.MEDIUM_WORD_LENGTH -> EngineConstants.MAX_COST_MEDIUM
            length <= EngineConstants.LONG_WORD_LENGTH -> EngineConstants.MAX_COST_LONG
            else -> EngineConstants.MAX_COST_VERY_LONG
        }
    }
}

/**
 * Weights edits by what's likely on a touch keyboard: hitting a neighbouring key, touching two adjacent keys at once,
 * doubling or missing a double letter and leaving out apostrophes are cheap.
 */
class KeyboardErrorModel(private val geometry: KeyboardGeometry) : ErrorModel {
    private val apostrophe = Alphabet.symbolOf(Alphabet.APOSTROPHE)

    override val transposition = EngineConstants.TRANSPOSITION

    override val firstLetterFactor = EngineConstants.FIRST_LETTER_FACTOR

    override fun substitution(typed: IntArray, i: Int, intended: Int) = geometry.substitutionCost(typed[i], intended)

    override fun insertion(typed: IntArray, i: Int): Float {
        val symbol = typed[i]
        val nearPrevious = i > 0 && (typed[i - 1] == symbol || geometry.areNeighbours(typed[i - 1], symbol))
        val nearNext = i + 1 < typed.size && geometry.areNeighbours(typed[i + 1], symbol)
        return if (nearPrevious || nearNext) EngineConstants.INS_NEAR else EngineConstants.INS_DEFAULT
    }

    override fun deletion(intended: Int, previous: Int): Float {
        return when (intended) {
            apostrophe -> EngineConstants.DEL_APOSTROPHE
            previous -> EngineConstants.DEL_DOUBLE
            else -> EngineConstants.DEL_DEFAULT
        }
    }
}

/** The cost of the edits from an intended word to a typed one, and the lowest cost from a prefix of it. */
class Alignment(val cost: Float, val prefixCost: Float)

/**
 * Aligns typed words with intended ones using the same weighted optimal string alignment as [FuzzySearch], which
 * computes it while walking the dictionary trie. Prefixes shorter than the typed word by more than a char don't count.
 */
class Aligner(private val model: ErrorModel, private val typed: IntArray, private val maxCost: Float) {
    private val columns = typed.size + 1
    private val insertions = FloatArray(typed.size) {
        model.insertion(typed, it) * if (it == 0) model.firstLetterFactor else 1f
    }
    private var rows = FloatArray(0)

    /** Aligns [intended] with the typed word. Costs over the limits of [maxCost] and prefixes are reported as MAX. */
    fun align(intended: IntArray): Alignment {
        val size = (intended.size + 1) * columns
        if (rows.size < size) rows = FloatArray(size)
        for (i in 1 until columns) {
            rows[i] = rows[i - 1] + insertions[i - 1]
        }

        val limit = maxOf(maxCost, EngineConstants.MAX_PREFIX_COST)
        var prefixCost = Float.MAX_VALUE
        for (depth in 1..intended.size) {
            val rowMin = fillRow(intended, depth)
            if (depth >= typed.size - 1) prefixCost = minOf(prefixCost, rows[depth * columns + typed.size])
            if (rowMin > limit) return Alignment(Float.MAX_VALUE, prefixCost)
        }

        return Alignment(rows[intended.size * columns + typed.size], prefixCost)
    }

    private fun fillRow(intended: IntArray, depth: Int): Float {
        val symbol = intended[depth - 1]
        val previousSymbol = if (depth > 1) intended[depth - 2] else Alphabet.NO_SYMBOL
        val deletion = model.deletion(symbol, previousSymbol) * if (depth == 1) model.firstLetterFactor else 1f
        val row = depth * columns
        val above = row - columns
        rows[row] = rows[above] + deletion
        var rowMin = rows[row]
        for (i in 1 until columns) {
            val typedSymbol = typed[i - 1]
            val factor = if (i == 1) model.firstLetterFactor else 1f
            val substitution = if (typedSymbol == symbol) 0f else model.substitution(typed, i - 1, symbol) * factor
            var cost = minOf(rows[above + i - 1] + substitution, rows[row + i - 1] + insertions[i - 1])
            cost = minOf(cost, rows[above + i] + deletion)
            val isTransposition = i > 1 && depth > 1 && typed[i - 2] == symbol && typedSymbol == previousSymbol
            if (isTransposition && typedSymbol != symbol) {
                cost = minOf(cost, rows[above - columns + i - 2] + model.transposition)
            }

            rows[row + i] = cost
            rowMin = minOf(rowMin, cost)
        }

        return rowMin
    }
}
