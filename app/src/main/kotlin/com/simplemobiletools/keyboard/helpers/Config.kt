package com.simplemobiletools.keyboard.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.keyboard.extensions.isDeviceLocked
import com.simplemobiletools.keyboard.extensions.safeStorageContext
import java.util.Locale

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context.safeStorageContext)
    }

    var vibrateOnKeypress: Boolean
        get() = prefs.getBoolean(VIBRATE_ON_KEYPRESS, true)
        set(vibrateOnKeypress) = prefs.edit().putBoolean(VIBRATE_ON_KEYPRESS, vibrateOnKeypress).apply()

    var showPopupOnKeypress: Boolean
        get() = prefs.getBoolean(SHOW_POPUP_ON_KEYPRESS, true)
        set(showPopupOnKeypress) = prefs.edit().putBoolean(SHOW_POPUP_ON_KEYPRESS, showPopupOnKeypress).apply()

    var enableSentencesCapitalization: Boolean
        get() = prefs.getBoolean(SENTENCES_CAPITALIZATION, true)
        set(enableCapitalization) = prefs.edit().putBoolean(SENTENCES_CAPITALIZATION, enableCapitalization).apply()

    var showKeyBorders: Boolean
        get() = prefs.getBoolean(SHOW_KEY_BORDERS, false)
        set(showKeyBorders) = prefs.edit().putBoolean(SHOW_KEY_BORDERS, showKeyBorders).apply()

    var lastExportedClipsFolder: String
        get() = prefs.getString(LAST_EXPORTED_CLIPS_FOLDER, "")!!
        set(lastExportedClipsFolder) = prefs.edit().putString(LAST_EXPORTED_CLIPS_FOLDER, lastExportedClipsFolder).apply()

    var keyboardLanguage: Int
        get() = prefs.getInt(KEYBOARD_LANGUAGE, getDefaultLanguage())
        set(keyboardLanguage) = prefs.edit().putInt(KEYBOARD_LANGUAGE, keyboardLanguage).apply()

    var keyboardHeightPercentage: Int
        get() = prefs.getInt(HEIGHT_PERCENTAGE, 100)
        set(keyboardHeightMultiplier) = prefs.edit().putInt(HEIGHT_PERCENTAGE, keyboardHeightMultiplier).apply()

    var showClipboardContent: Boolean
        get() = prefs.getBoolean(SHOW_CLIPBOARD_CONTENT, true)
        set(showClipboardContent) = prefs.edit().putBoolean(SHOW_CLIPBOARD_CONTENT, showClipboardContent).apply()

    var showNumbersRow: Boolean
        get() = if (context.isDeviceLocked) {
            true
        } else {
            prefs.getBoolean(SHOW_NUMBERS_ROW, false)
        }
        set(showNumbersRow) = prefs.edit().putBoolean(SHOW_NUMBERS_ROW, showNumbersRow).apply()

    var languageBengaliSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_BENGALI_SELECTED, false)
        set(languageBengaliSelected) = prefs.edit().putBoolean(LANGUAGE_BENGALI_SELECTED, languageBengaliSelected).apply()

    var languageBulgarianSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_BULGARIAN_SELECTED, false)
        set(languageBulgarianSelected) = prefs.edit().putBoolean(LANGUAGE_BULGARIAN_SELECTED, languageBulgarianSelected).apply()

    var languageDanishSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_DANISH_SELECTED, false)
        set(languageDanishSelected) = prefs.edit().putBoolean(LANGUAGE_DANISH_SELECTED, languageDanishSelected).apply()

    var languageEnglishQwertySelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_ENGLISH_QWERTY_SELECTED, false)
        set(languageEnglishQwertySelected) = prefs.edit().putBoolean(LANGUAGE_ENGLISH_QWERTY_SELECTED, languageEnglishQwertySelected).apply()

    var languageEnglishQwertzSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_ENGLISH_QWERTZ_SELECTED, false)
        set(languageEnglishQwertzSelected) = prefs.edit().putBoolean(LANGUAGE_ENGLISH_QWERTZ_SELECTED, languageEnglishQwertzSelected).apply()

    var languageEnglishDvorakSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_ENGLISH_DVORAK_SELECTED, false)
        set(languageEnglishDvorakSelected) = prefs.edit().putBoolean(LANGUAGE_ENGLISH_DVORAK_SELECTED, languageEnglishDvorakSelected).apply()

    var languageFrenchAzertySelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_FRENCH_AZERTY_SELECTED, false)
        set(languageFrenchAzertySelected) = prefs.edit().putBoolean(LANGUAGE_FRENCH_AZERTY_SELECTED, languageFrenchAzertySelected).apply()

    var languageFrenchBepoSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_FRENCH_BEPO_SELECTED, false)
        set(languageFrenchBepoSelected) = prefs.edit().putBoolean(LANGUAGE_FRENCH_BEPO_SELECTED, languageFrenchBepoSelected).apply()

    var languageGermanSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_GERMAN_SELECTED, false)
        set(languageGermanSelected) = prefs.edit().putBoolean(LANGUAGE_GERMAN_SELECTED, languageGermanSelected).apply()

    var languageGreekSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_GREEK_SELECTED, false)
        set(languageGreekSelected) = prefs.edit().putBoolean(LANGUAGE_GREEK_SELECTED, languageGreekSelected).apply()

    var languageLithuanianSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_LITHUANIAN_SELECTED, false)
        set(languageLithuanianSelected) = prefs.edit().putBoolean(LANGUAGE_LITHUANIAN_SELECTED, languageLithuanianSelected).apply()

    var languageNorwegianSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_NORWEGIAN_SELECTED, false)
        set(languageNorwegianSelected) = prefs.edit().putBoolean(LANGUAGE_NORWEGIAN_SELECTED, languageNorwegianSelected).apply()

    var languagePolishSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_POLISH_SELECTED, false)
        set(languagePolishSelected) = prefs.edit().putBoolean(LANGUAGE_POLISH_SELECTED, languagePolishSelected).apply()

    var languageRomanianSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_ROMANIAN_SELECTED, false)
        set(languageRomanianSelected) = prefs.edit().putBoolean(LANGUAGE_ROMANIAN_SELECTED, languageRomanianSelected).apply()

    var languageRussianSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_RUSSIAN_SELECTED, false)
        set(languageRussianSelected) = prefs.edit().putBoolean(LANGUAGE_RUSSIAN_SELECTED, languageRussianSelected).apply()

    var languageSlovenianSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_SLOVENIAN_SELECTED, false)
        set(languageSlovenianSelected) = prefs.edit().putBoolean(LANGUAGE_SLOVENIAN_SELECTED, languageSlovenianSelected).apply()

    var languageSpanishSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_SPANISH_SELECTED, false)
        set(languageSpanishSelected) = prefs.edit().putBoolean(LANGUAGE_SPANISH_SELECTED, languageSpanishSelected).apply()

    var languageSwedishSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_SWEDISH_SELECTED, false)
        set(languageSwedishSelected) = prefs.edit().putBoolean(LANGUAGE_SWEDISH_SELECTED, languageSwedishSelected).apply()

    var languageTurkishQSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_TURKISH_Q_SELECTED, false)
        set(languageTurkishQSelected) = prefs.edit().putBoolean(LANGUAGE_TURKISH_Q_SELECTED, languageTurkishQSelected).apply()

    var languageUkrainianSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_UKRAINIAN_SELECTED, false)
        set(languageUkrainianSelected) = prefs.edit().putBoolean(LANGUAGE_UKRAINIAN_SELECTED, languageUkrainianSelected).apply()

    var languageVietnameseTelexSelected: Boolean
        get() =  prefs.getBoolean(LANGUAGE_VIETNAMESE_TELEX_SELECTED, false)
        set(languageVietnameseTelexSelected) = prefs.edit().putBoolean(LANGUAGE_VIETNAMESE_TELEX_SELECTED, languageVietnameseTelexSelected).apply()

    private fun getDefaultLanguage(): Int {
        val conf = context.resources.configuration
        return if (conf.locale.toString().toLowerCase(Locale.getDefault()).startsWith("ru_")) {
            LANGUAGE_RUSSIAN
        } else {
            LANGUAGE_ENGLISH_QWERTY
        }
    }
}
