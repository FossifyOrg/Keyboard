package org.fossify.keyboard.dialogs

import android.view.View
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.getKeyboardLanguagesRadioItems

class ChangeLanguagePopup(
    inputView: View,
    private val onSelect: () -> Unit,
) {
    private val context = inputView.context
    private val config = context.config

    init {
        val items = context.getKeyboardLanguagesRadioItems()
        KeyboardRadioGroupDialog(inputView, items, config.keyboardLanguage) {
            config.keyboardLanguage = it as Int
            onSelect.invoke()
        }
    }
}
