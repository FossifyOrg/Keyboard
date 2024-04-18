package org.fossify.keyboard.dialogs

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.keyboard.R
import org.fossify.keyboard.activities.SettingsActivity
import org.fossify.keyboard.adapters.LanguageCheckboxItemAdapter
import org.fossify.keyboard.helpers.*

class SelectLanguagesToToggle(
    activity: SettingsActivity,
    private val onSelectionListener: (MutableSet<Int>) -> Unit
) {

    init {
        val dialogBuilder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.dialog_select_languages_to_toggle, null)
        dialogBuilder.setView(dialogView)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialog_select_languages_to_toggle)
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager

        // Keep this list alphabetical
        val languagesList = listOf(
            LANGUAGE_BENGALI,
            LANGUAGE_BULGARIAN,
            LANGUAGE_DANISH,
            LANGUAGE_ENGLISH_QWERTY,
            LANGUAGE_ENGLISH_QWERTZ,
            LANGUAGE_ENGLISH_DVORAK,
            LANGUAGE_FRENCH_AZERTY,
            LANGUAGE_FRENCH_BEPO,
            LANGUAGE_GERMAN,
            LANGUAGE_GREEK,
            LANGUAGE_LITHUANIAN,
            LANGUAGE_NORWEGIAN,
            LANGUAGE_POLISH,
            LANGUAGE_ROMANIAN,
            LANGUAGE_RUSSIAN,
            LANGUAGE_SLOVENIAN,
            LANGUAGE_SPANISH,
            LANGUAGE_SWEDISH,
            LANGUAGE_TURKISH_Q,
            LANGUAGE_UKRAINIAN,
            LANGUAGE_VIETNAMESE_TELEX
        )

        val adapter = LanguageCheckboxItemAdapter(languagesList, activity)
        recyclerView.adapter = adapter

        dialogBuilder.setPositiveButton(R.string.ok) { _, _ ->
            val selectedLanguages = adapter.getSelectedLanguages()
            onSelectionListener(selectedLanguages)
        }

        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.apply {
            activity.setupDialogStuff(dialogView, this)
        }
    }
}
