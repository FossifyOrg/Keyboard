package org.fossify.keyboard.suggestions

/**
 * Scores a candidate like a noisy channel model in log space: how common the word is, minus how unlikely the edits
 * from it to the typed word are, plus Jaro-Winkler similarity, which favours candidates that keep the typed prefix.
 */
class Ranker(
    private val costWeight: Float = EngineConstants.COST_WEIGHT,
    private val jaroWinklerWeight: Float = EngineConstants.JW_WEIGHT,
) {
    constructor(weights: EngineWeights) : this(weights.costWeight, weights.jwWeight)

    fun score(
        typedKey: String,
        candidateKey: String,
        freq: Int,
        cost: Float,
        isExact: Boolean,
        boost: Float = 0f,
    ): Float {
        var score = freq / EngineConstants.FREQ_SCALE - costWeight * cost + boost
        if (jaroWinklerWeight != 0f) score += jaroWinklerWeight * JaroWinkler.similarity(typedKey, candidateKey)
        if (isExact) score += EngineConstants.EXACT_BONUS
        return score
    }
}
