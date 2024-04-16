package org.fossify.keyboard.dialogs

import android.content.DialogInterface
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.keyboard.R
import org.fossify.keyboard.activities.SettingsActivity
import org.fossify.keyboard.databinding.DialogSelectLanguagesToToggleBinding
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.helpers.*

class SelectLanguagesToToggle(
    private val activity: SettingsActivity,
    private val onConfirm: (selectedLanguages: MutableSet<Int>) -> Unit,
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

            dialogLanguageBengali.isChecked = LANGUAGE_BENGALI in selectedLanguages
            dialogLanguageBulgarian.isChecked = LANGUAGE_BULGARIAN in selectedLanguages
            dialogLanguageDanish.isChecked = LANGUAGE_DANISH in config.selectedLanguages
            dialogLanguageEnglishQwerty.isChecked = LANGUAGE_ENGLISH_QWERTY in selectedLanguages
            dialogLanguageEnglishQwertz.isChecked = LANGUAGE_ENGLISH_QWERTZ in selectedLanguages
            dialogLanguageEnglishDvorak.isChecked = LANGUAGE_ENGLISH_DVORAK in selectedLanguages
            dialogLanguageFrenchAzerty.isChecked = LANGUAGE_FRENCH_AZERTY in selectedLanguages
            dialogLanguageFrenchBepo.isChecked = LANGUAGE_FRENCH_BEPO in selectedLanguages
            dialogLanguageGerman.isChecked = LANGUAGE_GERMAN in selectedLanguages
            dialogLanguageGreek.isChecked = LANGUAGE_GREEK in selectedLanguages
            dialogLanguageLithuanian.isChecked = LANGUAGE_LITHUANIAN in selectedLanguages
            dialogLanguageNorwegian.isChecked = LANGUAGE_NORWEGIAN in selectedLanguages
            dialogLanguagePolish.isChecked = LANGUAGE_POLISH in selectedLanguages
            dialogLanguageRomanian.isChecked = LANGUAGE_ROMANIAN in selectedLanguages
            dialogLanguageRussian.isChecked = LANGUAGE_RUSSIAN in selectedLanguages
            dialogLanguageSlovenian.isChecked = LANGUAGE_SLOVENIAN in selectedLanguages
            dialogLanguageSpanish.isChecked = LANGUAGE_SPANISH in selectedLanguages
            dialogLanguageSwedish.isChecked = LANGUAGE_SWEDISH in selectedLanguages
            dialogLanguageTurkishQ.isChecked = LANGUAGE_TURKISH_Q in selectedLanguages
            dialogLanguageUkrainian.isChecked = LANGUAGE_UKRAINIAN in selectedLanguages
            dialogLanguageVietnameseTelex.isChecked = LANGUAGE_VIETNAMESE_TELEX in selectedLanguages

            dialogLanguageBengaliHolder.setOnClickListener {
                val language = LANGUAGE_BENGALI
                dialogLanguageBengali.isChecked = !dialogLanguageBengali.isChecked
                if (dialogLanguageBengali.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }

            dialogLanguageBulgarianHolder.setOnClickListener {
                val language = LANGUAGE_BULGARIAN
                dialogLanguageBulgarian.isChecked = !dialogLanguageBulgarian.isChecked
                if (dialogLanguageBulgarian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageDanishHolder.setOnClickListener {
                val language = LANGUAGE_DANISH
                dialogLanguageDanish.isChecked = !dialogLanguageDanish.isChecked
                if (dialogLanguageDanish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageEnglishQwertyHolder.setOnClickListener {
                val language = LANGUAGE_ENGLISH_QWERTY
                dialogLanguageEnglishQwerty.isChecked = !dialogLanguageEnglishQwerty.isChecked
                if (dialogLanguageEnglishQwerty.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageEnglishQwertzHolder.setOnClickListener {
                val language = LANGUAGE_ENGLISH_QWERTZ
                dialogLanguageEnglishQwertz.isChecked = !dialogLanguageEnglishQwertz.isChecked
                if (dialogLanguageEnglishQwertz.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageEnglishDvorakHolder.setOnClickListener {
                val language = LANGUAGE_ENGLISH_DVORAK
                dialogLanguageEnglishDvorak.isChecked = !dialogLanguageEnglishDvorak.isChecked
                if (dialogLanguageEnglishDvorak.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageFrenchAzertyHolder.setOnClickListener {
                val language = LANGUAGE_FRENCH_AZERTY
                dialogLanguageFrenchAzerty.isChecked = !dialogLanguageFrenchAzerty.isChecked
                if (dialogLanguageFrenchAzerty.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageFrenchBepoHolder.setOnClickListener {
                val language = LANGUAGE_FRENCH_BEPO
                dialogLanguageFrenchBepo.isChecked = !dialogLanguageFrenchBepo.isChecked
                if (dialogLanguageFrenchBepo.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageGermanHolder.setOnClickListener {
                val language = LANGUAGE_GERMAN
                dialogLanguageGerman.isChecked = !dialogLanguageGerman.isChecked
                if (dialogLanguageGerman.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageGreekHolder.setOnClickListener {
                val language = LANGUAGE_GREEK
                dialogLanguageGreek.isChecked = !dialogLanguageGreek.isChecked
                if (dialogLanguageGreek.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageLithuanianHolder.setOnClickListener {
                val language = LANGUAGE_LITHUANIAN
                dialogLanguageLithuanian.isChecked = !dialogLanguageLithuanian.isChecked
                if (dialogLanguageLithuanian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageNorwegianHolder.setOnClickListener {
                val language = LANGUAGE_NORWEGIAN
                dialogLanguageNorwegian.isChecked = !dialogLanguageNorwegian.isChecked
                if (dialogLanguageNorwegian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguagePolishHolder.setOnClickListener {
                val language = LANGUAGE_POLISH
                dialogLanguagePolish.isChecked = !dialogLanguagePolish.isChecked
                if (dialogLanguagePolish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageRomanianHolder.setOnClickListener {
                val language = LANGUAGE_ROMANIAN
                dialogLanguageRomanian.isChecked = !dialogLanguageRomanian.isChecked
                if (dialogLanguageRomanian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageRussianHolder.setOnClickListener {
                val language = LANGUAGE_RUSSIAN
                dialogLanguageRussian.isChecked = !dialogLanguageRussian.isChecked
                if (dialogLanguageRussian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageSlovenianHolder.setOnClickListener {
                val language = LANGUAGE_SLOVENIAN
                dialogLanguageSlovenian.isChecked = !dialogLanguageSlovenian.isChecked
                if (dialogLanguageSlovenian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageSpanishHolder.setOnClickListener {
                val language = LANGUAGE_SPANISH
                dialogLanguageSpanish.isChecked = !dialogLanguageSpanish.isChecked
                if (dialogLanguageSpanish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageSwedishHolder.setOnClickListener {
                val language = LANGUAGE_SWEDISH
                dialogLanguageSwedish.isChecked = !dialogLanguageSwedish.isChecked
                if (dialogLanguageSwedish.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageTurkishQHolder.setOnClickListener {
                val language = LANGUAGE_TURKISH_Q
                dialogLanguageTurkishQ.isChecked = !dialogLanguageTurkishQ.isChecked
                if (dialogLanguageTurkishQ.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageUkrainianHolder.setOnClickListener {
                val language = LANGUAGE_UKRAINIAN
                dialogLanguageUkrainian.isChecked = !dialogLanguageUkrainian.isChecked
                if (dialogLanguageUkrainian.isChecked) {
                    selectedLanguages.add(language)
                } else {
                    selectedLanguages.remove(language)
                }
            }
            dialogLanguageVietnameseTelexHolder.setOnClickListener {
                val language = LANGUAGE_VIETNAMESE_TELEX
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
        val defaultLang = config.getDefaultLanguage()
        if (selectedLanguages.size == 0) {
            selectedLanguages.add(defaultLang)
        }
        config.selectedLanguages = selectedLanguages
        onConfirm(selectedLanguages)
    }
}
