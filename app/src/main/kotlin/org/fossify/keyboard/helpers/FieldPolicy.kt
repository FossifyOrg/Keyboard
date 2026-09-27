package org.fossify.keyboard.helpers

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Which word suggestion features a text field allows. */
data class FieldPolicy(val suggestions: Boolean, val autocorrect: Boolean, val learning: Boolean) {
    companion object {
        val DISABLED = FieldPolicy(suggestions = false, autocorrect = false, learning = false)

        private val PRIVATE_VARIATIONS = setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
        )

        private val NO_AUTOCORRECT_VARIATIONS = setOf(
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_FILTER,
        )

        fun from(editorInfo: EditorInfo?, isDeviceLocked: Boolean): FieldPolicy {
            return if (editorInfo == null) {
                DISABLED
            } else {
                from(editorInfo.inputType, editorInfo.imeOptions, isDeviceLocked)
            }
        }

        fun from(inputType: Int, imeOptions: Int, isDeviceLocked: Boolean): FieldPolicy {
            if (isDeviceLocked || !allowsSuggestions(inputType)) return DISABLED

            return FieldPolicy(
                suggestions = true,
                autocorrect = inputType and InputType.TYPE_MASK_VARIATION !in NO_AUTOCORRECT_VARIATIONS,
                learning = imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0,
            )
        }

        private fun allowsSuggestions(inputType: Int): Boolean {
            val isText = inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT
            val isPrivate = inputType and InputType.TYPE_MASK_VARIATION in PRIVATE_VARIATIONS
            val noSuggestions = inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0
            return isText && !isPrivate && !noSuggestions
        }
    }
}
