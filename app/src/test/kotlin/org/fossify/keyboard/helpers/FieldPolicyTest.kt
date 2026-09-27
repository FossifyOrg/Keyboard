package org.fossify.keyboard.helpers

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FieldPolicyTest {

    private val allOn = FieldPolicy(suggestions = true, autocorrect = true, learning = true)

    private fun text(variation: Int = InputType.TYPE_TEXT_VARIATION_NORMAL, flags: Int = 0) =
        InputType.TYPE_CLASS_TEXT or variation or flags

    @Test
    fun plainTextAllowsEverything() {
        assertEquals(allOn, FieldPolicy.from(text(), 0, isDeviceLocked = false))
        assertEquals(allOn, FieldPolicy.from(text(flags = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES), 0, false))
        assertEquals(allOn, FieldPolicy.from(text(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE), 0, false))
    }

    @Test
    fun privateAndNonTextFieldsAllowNothing() {
        val disabled = listOf(
            text(InputType.TYPE_TEXT_VARIATION_PASSWORD),
            text(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD),
            text(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD),
            text(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
            text(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS),
            text(InputType.TYPE_TEXT_VARIATION_URI),
            text(flags = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME,
            InputType.TYPE_NULL,
        )
        for (inputType in disabled) {
            assertEquals(inputType.toString(), FieldPolicy.DISABLED, FieldPolicy.from(inputType, 0, false))
        }

        assertEquals(FieldPolicy.DISABLED, FieldPolicy.from(text(), 0, isDeviceLocked = true))
    }

    @Test
    fun namesAndAddressesAreNotAutocorrected() {
        for (variation in listOf(
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_FILTER,
        )) {
            assertEquals(allOn.copy(autocorrect = false), FieldPolicy.from(text(variation), 0, false))
        }
    }

    @Test
    fun incognitoFieldsAreNotLearnedFrom() {
        val policy = FieldPolicy.from(text(), EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, false)
        assertEquals(allOn.copy(learning = false), policy)
    }
}
