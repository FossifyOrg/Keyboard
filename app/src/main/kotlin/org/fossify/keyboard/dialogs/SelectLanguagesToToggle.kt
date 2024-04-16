package org.fossify.keyboard.dialogs

import android.content.DialogInterface
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.keyboard.R
import org.fossify.keyboard.activities.SettingsActivity
import org.fossify.keyboard.databinding.DialogSelectLanguagesToToggleBinding
import org.fossify.keyboard.extensions.*

class SelectLanguagesToToggle(
    val activity: SettingsActivity,
    private val onConfirm: (selectedLanguages: MutableSet<String>) -> Unit,
) : DialogInterface.OnClickListener {
    private var config = activity.config
    private val binding: DialogSelectLanguagesToToggleBinding
    private val selectedLanguages = config.selectedLanguages

    init {
        binding = DialogSelectLanguagesToToggleBinding.inflate(activity.layoutInflater).apply {
            dialogLanguageEnglishQwerty.text = "${activity.getString(R.string.translation_english)} (QWERTY)"
            dialogLanguageEnglishQwertz.text = "${activity.getString(R.string.translation_english)} (QWERTZ)"
            dialogLanguageEnglishDvorak.text = "${activity.getString(R.string.translation_english)} (DVORAK)"
            dialogLanguageFrenchAzerty.text = "${activity.getString(R.string.translation_french)} (AZERTY)"
            dialogLanguageFrenchBepo.text = "${activity.getString(R.string.translation_french)} (BEPO)"
            dialogLanguageTurkishQ.text = "${activity.getString(R.string.translation_turkish)} (Q)"
            dialogLanguageVietnameseTelex.text = "${activity.getString(R.string.translation_vietnamese)} (TELEX)"

            dialogLanguageBengali.isChecked = "bengali" in config.selectedLanguages
            dialogLanguageBulgarian.isChecked = "bulgarian" in config.selectedLanguages
            dialogLanguageDanish.isChecked = "danish" in config.selectedLanguages
            dialogLanguageEnglishQwerty.isChecked = "english_qwerty" in config.selectedLanguages
            dialogLanguageEnglishQwertz.isChecked = "english_qwertz" in config.selectedLanguages
            dialogLanguageEnglishDvorak.isChecked = "english_dvorak" in config.selectedLanguages
            dialogLanguageFrenchAzerty.isChecked = "french_azerty" in config.selectedLanguages
            dialogLanguageFrenchBepo.isChecked = "french_bepo" in config.selectedLanguages
            dialogLanguageGerman.isChecked = "german" in config.selectedLanguages
            dialogLanguageGreek.isChecked = "greek" in config.selectedLanguages
            dialogLanguageLithuanian.isChecked = "lithuanian" in config.selectedLanguages
            dialogLanguageNorwegian.isChecked = "norwegian" in config.selectedLanguages
            dialogLanguagePolish.isChecked = "polish" in config.selectedLanguages
            dialogLanguageRomanian.isChecked = "romanian" in config.selectedLanguages
            dialogLanguageRussian.isChecked = "russian" in config.selectedLanguages
            dialogLanguageSlovenian.isChecked = "slovenian" in config.selectedLanguages
            dialogLanguageSpanish.isChecked = "spanish" in config.selectedLanguages
            dialogLanguageSwedish.isChecked = "swedish" in config.selectedLanguages
            dialogLanguageTurkishQ.isChecked = "turkish_q" in config.selectedLanguages
            dialogLanguageUkrainian.isChecked = "ukrainian" in config.selectedLanguages
            dialogLanguageVietnameseTelex.isChecked = "vietnamese_telex" in config.selectedLanguages

            dialogLanguageBengaliHolder.setOnClickListener {
                val language = "bengali"
                dialogLanguageBengali.isChecked = !dialogLanguageBengali.isChecked
                if (dialogLanguageBengali.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }

            dialogLanguageBulgarianHolder.setOnClickListener {
                val language = "bulgarian"
                dialogLanguageBulgarian.isChecked = !dialogLanguageBulgarian.isChecked
                if (dialogLanguageBulgarian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageDanishHolder.setOnClickListener {
                val language = "danish"
                dialogLanguageDanish.isChecked = !dialogLanguageDanish.isChecked
                if (dialogLanguageDanish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageEnglishQwertyHolder.setOnClickListener {
                val language = "english_qwerty"
                dialogLanguageEnglishQwerty.isChecked = !dialogLanguageEnglishQwerty.isChecked
                if (dialogLanguageEnglishQwerty.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageEnglishQwertzHolder.setOnClickListener {
                val language = "english_qwertz"
                dialogLanguageEnglishQwertz.isChecked = !dialogLanguageEnglishQwertz.isChecked
                if (dialogLanguageEnglishQwertz.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageEnglishDvorakHolder.setOnClickListener {
                val language = "english_dvorak"
                dialogLanguageEnglishDvorak.isChecked = !dialogLanguageEnglishDvorak.isChecked
                if (dialogLanguageEnglishDvorak.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageFrenchAzertyHolder.setOnClickListener {
                val language = "french_azerty"
                dialogLanguageFrenchAzerty.isChecked = !dialogLanguageFrenchAzerty.isChecked
                if (dialogLanguageFrenchAzerty.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageFrenchBepoHolder.setOnClickListener {
                val language = "english_bepo"
                dialogLanguageFrenchBepo.isChecked = !dialogLanguageFrenchBepo.isChecked
                if (dialogLanguageFrenchBepo.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageGermanHolder.setOnClickListener {
                val language = "german"
                dialogLanguageGerman.isChecked = !dialogLanguageGerman.isChecked
                if (dialogLanguageGerman.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageGreekHolder.setOnClickListener {
                val language = "greek"
                dialogLanguageGreek.isChecked = !dialogLanguageGreek.isChecked
                if (dialogLanguageGreek.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageLithuanianHolder.setOnClickListener {
                val language = "lithuanian"
                dialogLanguageLithuanian.isChecked = !dialogLanguageLithuanian.isChecked
                if (dialogLanguageLithuanian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageNorwegianHolder.setOnClickListener {
                val language = "norwegian"
                dialogLanguageNorwegian.isChecked = !dialogLanguageNorwegian.isChecked
                if (dialogLanguageNorwegian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguagePolishHolder.setOnClickListener {
                val language = "polish"
                dialogLanguagePolish.isChecked = !dialogLanguagePolish.isChecked
                if (dialogLanguagePolish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageRomanianHolder.setOnClickListener {
                val language = "romanian"
                dialogLanguageRomanian.isChecked = !dialogLanguageRomanian.isChecked
                if (dialogLanguageRomanian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageRussianHolder.setOnClickListener {
                val language = "russian"
                dialogLanguageRussian.isChecked = !dialogLanguageRussian.isChecked
                if (dialogLanguageRussian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageSlovenianHolder.setOnClickListener {
                val language = "slovenian"
                dialogLanguageSlovenian.isChecked = !dialogLanguageSlovenian.isChecked
                if (dialogLanguageSlovenian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageSpanishHolder.setOnClickListener {
                val language = "spanish"
                dialogLanguageSpanish.isChecked = !dialogLanguageSpanish.isChecked
                if (dialogLanguageSpanish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageSwedishHolder.setOnClickListener {
                val language = "swedish"
                dialogLanguageSwedish.isChecked = !dialogLanguageSwedish.isChecked
                if (dialogLanguageSwedish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageTurkishQHolder.setOnClickListener {
                val language = "turkish_q"
                dialogLanguageTurkishQ.isChecked = !dialogLanguageTurkishQ.isChecked
                if (dialogLanguageTurkishQ.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageUkrainianHolder.setOnClickListener {
                val language = "ukrainian"
                dialogLanguageUkrainian.isChecked = !dialogLanguageUkrainian.isChecked
                if (dialogLanguageUkrainian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageVietnameseTelexHolder.setOnClickListener {
                val language = "vietnamese_telex"
                dialogLanguageVietnameseTelex.isChecked = !dialogLanguageVietnameseTelex.isChecked
                if (dialogLanguageVietnameseTelex.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        config.selectedLanguages = selectedLanguages
        onConfirm(selectedLanguages)
    }
}
