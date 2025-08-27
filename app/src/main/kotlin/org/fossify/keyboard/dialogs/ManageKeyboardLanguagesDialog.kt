package org.fossify.keyboard.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.keyboard.R
import org.fossify.keyboard.adapters.ManageKeyboardLanguagesAdapter
import org.fossify.keyboard.databinding.DialogManageKeyboardLanguagesBinding
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.getKeyboardLanguageText
import org.fossify.keyboard.helpers.SUPPORTED_LANGUAGES

class ManageKeyboardLanguagesDialog(
    private val activity: BaseSimpleActivity,
    private val callback: () -> Unit
) {
    init {
        val binding = DialogManageKeyboardLanguagesBinding.inflate(activity.layoutInflater)
        val languageItems = SUPPORTED_LANGUAGES.map {
            it to activity.getKeyboardLanguageText(it)
        }.sortedBy { it.second }

        val adapter = ManageKeyboardLanguagesAdapter(activity.config, languageItems)
        binding.keyboardLanguageList.adapter = adapter

        activity.getAlertDialogBuilder()
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.ok) { _, _ ->
                val selectedLanguages = adapter.getSelectedLanguages()
                activity.config.selectedLanguages = selectedLanguages
                if (activity.config.keyboardLanguage !in selectedLanguages) {
                    activity.config.keyboardLanguage = selectedLanguages.first()
                }

                callback()
            }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
