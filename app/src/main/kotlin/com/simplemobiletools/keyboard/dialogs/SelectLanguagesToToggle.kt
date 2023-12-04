package com.simplemobiletools.keyboard.dialogs

import android.content.DialogInterface
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.keyboard.R
import com.simplemobiletools.keyboard.activities.SettingsActivity
import com.simplemobiletools.keyboard.databinding.DialogSelectLangugesToToggleBinding
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.extensions.setupKeyboardDialogStuff

class SelectLanguagesToToggle(
    val activity: SettingsActivity,
) : DialogInterface.OnClickListener {
    private var config = activity.config
    private val binding: DialogSelectLangugesToToggleBinding

    init {
        binding = DialogSelectLangugesToToggleBinding.inflate(activity.layoutInflater).apply {
            dialogLanguageEnglishQwerty.text = "${activity.getString(R.string.translation_english)} (QWERTY)"
            dialogLanguageEnglishQwertz.text = "${activity.getString(R.string.translation_english)} (QWERTZ)"
            dialogLanguageEnglishDvorak.text = "${activity.getString(R.string.translation_english)} (DVORAK)"
            dialogLanguageFrenchAzerty.text = "${activity.getString(R.string.translation_french)} (AZERTY)"
            dialogLanguageFrenchBepo.text = "${activity.getString(R.string.translation_french)} (BEPO)"
            dialogLanguageTurkishQ.text = "${activity.getString(R.string.translation_turkish)} (Q)"
            dialogLanguageVietnameseTelex.text = "${activity.getString(R.string.translation_vietnamese)} (TELEX)"

            dialogLanguageBengali.isChecked = config.languageBengaliSelected
            dialogLanguageBulgarian.isChecked = config.languageBulgarianSelected
            dialogLanguageDanish.isChecked = config.languageDanishSelected
            dialogLanguageEnglishQwerty.isChecked = config.languageEnglishQwertySelected
            dialogLanguageEnglishQwertz.isChecked = config.languageEnglishQwertzSelected
            dialogLanguageFrenchAzerty.isChecked = config.languageFrenchAzertySelected
            dialogLanguageFrenchBepo.isChecked = config.languageFrenchBepoSelected
            dialogLanguageGerman.isChecked = config.languageGermanSelected
            dialogLanguageGreek.isChecked = config.languageGreekSelected
            dialogLanguageLithuanian.isChecked = config.languageLithuanianSelected
            dialogLanguageNorwegian.isChecked = config.languageNorwegianSelected
            dialogLanguagePolish.isChecked = config.languagePolishSelected
            dialogLanguageRomanian.isChecked = config.languageRomanianSelected
            dialogLanguageRussian.isChecked = config.languageRussianSelected
            dialogLanguageSlovenian.isChecked = config.languageSlovenianSelected
            dialogLanguageSpanish.isChecked = config.languageSpanishSelected
            dialogLanguageSwedish.isChecked = config.languageSwedishSelected
            dialogLanguageTurkishQ.isChecked = config.languageTurkishQSelected
            dialogLanguageUkrainian.isChecked = config.languageUkrainianSelected
            dialogLanguageVietnameseTelex.isChecked = config.languageVietnameseTelexSelected

            dialogLanguageBengaliHolder.setOnClickListener { dialogLanguageBengali.toggle()}
            dialogLanguageBulgarianHolder.setOnClickListener { dialogLanguageBulgarian.toggle()}
            dialogLanguageDanishHolder.setOnClickListener { dialogLanguageDanish.toggle()}
            dialogLanguageEnglishQwertyHolder.setOnClickListener { dialogLanguageEnglishQwerty.toggle()}
            dialogLanguageEnglishQwertzHolder.setOnClickListener { dialogLanguageEnglishQwertz.toggle()}
            dialogLanguageEnglishDvorakHolder.setOnClickListener { dialogLanguageEnglishDvorak.toggle() }
            dialogLanguageFrenchAzertyHolder.setOnClickListener { dialogLanguageFrenchAzerty.toggle()}
            dialogLanguageFrenchBepoHolder.setOnClickListener { dialogLanguageFrenchBepo.toggle()}
            dialogLanguageGermanHolder.setOnClickListener { dialogLanguageGerman.toggle()}
            dialogLanguageGreekHolder.setOnClickListener { dialogLanguageGreek.toggle()}
            dialogLanguageLithuanianHolder.setOnClickListener { dialogLanguageLithuanian.toggle()}
            dialogLanguageNorwegianHolder.setOnClickListener { dialogLanguageNorwegian.toggle()}
            dialogLanguagePolishHolder.setOnClickListener { dialogLanguagePolish.toggle()}
            dialogLanguageRomanianHolder.setOnClickListener { dialogLanguageRomanian.toggle()}
            dialogLanguageRussianHolder.setOnClickListener { dialogLanguageRussian.toggle()}
            dialogLanguageSlovenianHolder.setOnClickListener { dialogLanguageSlovenian.toggle()}
            dialogLanguageSpanishHolder.setOnClickListener { dialogLanguageSpanish.toggle()}
            dialogLanguageSwedishHolder.setOnClickListener { dialogLanguageSwedish.toggle()}
            dialogLanguageTurkishQHolder.setOnClickListener { dialogLanguageTurkishQ.toggle()}
            dialogLanguageUkrainianHolder.setOnClickListener { dialogLanguageUkrainian.toggle()}
            dialogLanguageVietnameseTelexHolder.setOnClickListener { dialogLanguageVietnameseTelex.toggle()}
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, this)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        config.languageBengaliSelected = binding.dialogLanguageBengali.isChecked
        config.languageBulgarianSelected = binding.dialogLanguageBulgarian.isChecked
        config.languageDanishSelected = binding.dialogLanguageDanish.isChecked
        config.languageEnglishQwertySelected = binding.dialogLanguageEnglishQwerty.isChecked
        config.languageEnglishQwertzSelected = binding.dialogLanguageEnglishQwertz.isChecked
        config.languageEnglishDvorakSelected = binding.dialogLanguageEnglishDvorak.isChecked
        config.languageFrenchAzertySelected = binding.dialogLanguageFrenchAzerty.isChecked
        config.languageFrenchBepoSelected = binding.dialogLanguageFrenchBepo.isChecked
        config.languageGermanSelected = binding.dialogLanguageGerman.isChecked
        config.languageGreekSelected = binding.dialogLanguageGreek.isChecked
        config.languageLithuanianSelected = binding.dialogLanguageLithuanian.isChecked
        config.languageNorwegianSelected = binding.dialogLanguageNorwegian.isChecked
        config.languagePolishSelected = binding.dialogLanguagePolish.isChecked
        config.languageRomanianSelected = binding.dialogLanguageRomanian.isChecked
        config.languageRussianSelected = binding.dialogLanguageRussian.isChecked
        config.languageSlovenianSelected = binding.dialogLanguageSlovenian.isChecked
        config.languageSpanishSelected = binding.dialogLanguageSpanish.isChecked
        config.languageSwedishSelected = binding.dialogLanguageSwedish.isChecked
        config.languageTurkishQSelected = binding.dialogLanguageTurkishQ.isChecked
        config.languageUkrainianSelected = binding.dialogLanguageUkrainian.isChecked
        config.languageVietnameseTelexSelected = binding.dialogLanguageVietnameseTelex.isChecked
    }
}
