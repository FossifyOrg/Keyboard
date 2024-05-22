package org.fossify.keyboard.helpers

import android.content.Context
import org.fossify.commons.helpers.BaseConfig
import org.fossify.keyboard.extensions.isDeviceLocked
import org.fossify.keyboard.extensions.safeStorageContext
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

    var selectedLanguages: MutableSet<Int>
        get() {
            val defaultLanguage = getDefaultLanguage().toString()
            val stringSet = prefs.getStringSet(SELECTED_LANGUAGES, hashSetOf(defaultLanguage))!!
            return stringSet.map { it.toInt() }.toMutableSet()
        }
        set(selectedLanguages) {
            val stringSet = selectedLanguages.map { it.toString() }.toSet()
            prefs.edit().putStringSet(SELECTED_LANGUAGES, stringSet).apply()
        }

    fun getDefaultLanguage(): Int {
        val conf = context.resources.configuration
        return if (conf.locale.toString().lowercase(Locale.getDefault()).startsWith("ru_")) {
            LANGUAGE_RUSSIAN
        } else {
            LANGUAGE_ENGLISH_QWERTY
        }
    }
}
