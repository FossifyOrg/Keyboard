package org.fossify.keyboard.suggestions

/**
 * The error model and ranking weights that are tuned for each language by the weights study of the tests. Costs are in
 * edits, like in [EngineConstants], whose English values are the defaults.
 */
data class EngineWeights(
    val subBase: Float = EngineConstants.SUB_BASE,
    val subPerKey: Float = EngineConstants.SUB_PER_KEY,
    val insNear: Float = EngineConstants.INS_NEAR,
    val delApostrophe: Float = EngineConstants.DEL_APOSTROPHE,
    val delDouble: Float = EngineConstants.DEL_DOUBLE,
    val transposition: Float = EngineConstants.TRANSPOSITION,
    val firstLetterFactor: Float = EngineConstants.FIRST_LETTER_FACTOR,
    val costWeight: Float = EngineConstants.COST_WEIGHT,
    val jwWeight: Float = EngineConstants.JW_WEIGHT,
    val margin: Float = EngineConstants.AUTOCORRECT_MARGIN,
) {
    companion object {
        val ENGLISH = EngineWeights()
    }
}
