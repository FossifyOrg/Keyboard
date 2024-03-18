package org.fossify.keyboard.dialogs

import android.content.DialogInterface
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.keyboard.R
import org.fossify.keyboard.activities.SettingsActivity
import org.fossify.keyboard.databinding.DialogSelectLanguagesToToggleBinding
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.setupKeyboardDialogStuff
class SelectLanguagesToToggle(
    val activity: SettingsActivity,
) : DialogInterface.OnClickListener {
    private var config = activity.config
    private val binding: DialogSelectLanguagesToToggleBinding
    private val selectedLanguages = mutableSetOf<String>()

    init {
        binding = DialogSelectLanguagesToToggleBinding.inflate(activity.layoutInflater).apply {
            dialogLanguageEnglishQwerty.text = "${activity.getString(R.string.translation_english)} (QWERTY)"
            dialogLanguageEnglishQwertz.text = "${activity.getString(R.string.translation_english)} (QWERTZ)"
            dialogLanguageEnglishDvorak.text = "${activity.getString(R.string.translation_english)} (DVORAK)"
            dialogLanguageFrenchAzerty.text = "${activity.getString(R.string.translation_french)} (AZERTY)"
            dialogLanguageFrenchBepo.text = "${activity.getString(R.string.translation_french)} (BEPO)"
            dialogLanguageTurkishQ.text = "${activity.getString(R.string.translation_turkish)} (Q)"
            dialogLanguageVietnameseTelex.text = "${activity.getString(R.string.translation_vietnamese)} (TELEX)"

            dialogLanguageBengali.isChecked = selectedLanguages.add("bengali")
            dialogLanguageBulgarian.isChecked = selectedLanguages.add("bulgarian")
            dialogLanguageDanish.isChecked = selectedLanguages.add("danish")
            dialogLanguageEnglishQwerty.isChecked = selectedLanguages.add("english_qwerty")
            dialogLanguageEnglishQwertz.isChecked = selectedLanguages.add("english_qwertz")
            dialogLanguageEnglishDvorak.isChecked = selectedLanguages.add("english_dvorak")
            dialogLanguageFrenchAzerty.isChecked = selectedLanguages.add("english_azerty")
            dialogLanguageFrenchBepo.isChecked =  selectedLanguages.add("french_bepo")
            dialogLanguageGerman.isChecked =  selectedLanguages.add("german")
            dialogLanguageGreek.isChecked = selectedLanguages.add("greek")
            dialogLanguageLithuanian.isChecked =  selectedLanguages.add("lithuanian")
            dialogLanguageNorwegian.isChecked = selectedLanguages.add("norwegian")
            dialogLanguagePolish.isChecked =  selectedLanguages.add("polish")
            dialogLanguageRomanian.isChecked = selectedLanguages.add("romanian")
            dialogLanguageRussian.isChecked = selectedLanguages.add("russian")
            dialogLanguageSlovenian.isChecked =  selectedLanguages.add("slovenian")
            dialogLanguageSpanish.isChecked =  selectedLanguages.add("spanish")
            dialogLanguageSwedish.isChecked =  selectedLanguages.add("swedish")
            dialogLanguageTurkishQ.isChecked =  selectedLanguages.add("turkish_q")
            dialogLanguageUkrainian.isChecked =  selectedLanguages.add("ukrainian")
            dialogLanguageVietnameseTelex.isChecked = selectedLanguages.add("vietnamese_telex")

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
            .setPositiveButton(org.fossify.commons.R.string.ok, this)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        config.selectedLanguages = selectedLanguages
    }
}
