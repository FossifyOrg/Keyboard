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

    var soundOnKeypress: Int
        get() = prefs.getInt(SOUND_ON_KEYPRESS, SOUND_SYSTEM)
        set(soundOnKeypress) = prefs.edit().putInt(SOUND_ON_KEYPRESS, soundOnKeypress).apply()

    var showPopupOnKeypress: Boolean
        get() = prefs.getBoolean(SHOW_POPUP_ON_KEYPRESS, true)
        set(showPopupOnKeypress) = prefs.edit().putBoolean(SHOW_POPUP_ON_KEYPRESS, showPopupOnKeypress).apply()

    var enableSentencesCapitalization: Boolean
        get() = prefs.getBoolean(SENTENCES_CAPITALIZATION, true)
        set(enableCapitalization) = prefs.edit().putBoolean(SENTENCES_CAPITALIZATION, enableCapitalization).apply()

    var showEmojiKey: Boolean
        get() = prefs.getBoolean(SHOW_EMOJI_KEY, true)
        set(showEmojiKey) = prefs.edit().putBoolean(SHOW_EMOJI_KEY, showEmojiKey).apply()

    var showLanguageSwitchKey: Boolean
        get() = prefs.getBoolean(SHOW_LANGUAGE_SWITCH_KEY, false)
        set(showLanguageSwitchKey) = prefs.edit().putBoolean(SHOW_LANGUAGE_SWITCH_KEY, showLanguageSwitchKey).apply()

    var showKeyBorders: Boolean
        get() = prefs.getBoolean(SHOW_KEY_BORDERS, true)
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

    var voiceInputMethod: String
        get() = prefs.getString(VOICE_INPUT_METHOD, "")!!
        set(voiceInputMethod) = prefs.edit().putString(VOICE_INPUT_METHOD, voiceInputMethod).apply()

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

    fun getDefaultLanguage(): Int = languageForLocale(context.resources.configuration.locale)

    /** maps the system locale to the closest supported keyboard language (English QWERTY as fallback) */
    private fun languageForLocale(locale: Locale): Int {
        val country = locale.country.uppercase(Locale.getDefault())
        return when (locale.language.lowercase(Locale.getDefault())) {
            "fr" -> LANGUAGE_FRENCH_AZERTY
            "de" -> if (country == "DE") LANGUAGE_GERMAN else LANGUAGE_GERMAN_QWERTZ
            "es" -> LANGUAGE_SPANISH
            "it" -> LANGUAGE_ITALIAN
            "pt" -> LANGUAGE_PORTUGUESE
            "nl" -> LANGUAGE_DUTCH
            "pl" -> LANGUAGE_POLISH
            "sv" -> LANGUAGE_SWEDISH
            "da" -> LANGUAGE_DANISH
            "nb", "no" -> LANGUAGE_NORWEGIAN
            "ru" -> LANGUAGE_RUSSIAN
            "uk" -> LANGUAGE_UKRAINIAN
            "cs" -> LANGUAGE_CZECH_QWERTY
            "tr" -> LANGUAGE_TURKISH
            "el" -> LANGUAGE_GREEK
            "he" -> LANGUAGE_HEBREW
            "ar" -> LANGUAGE_ARABIC
            "bn" -> LANGUAGE_BENGALI
            "lv" -> LANGUAGE_LATVIAN
            "lt" -> LANGUAGE_LITHUANIAN
            "ro" -> LANGUAGE_ROMANIAN
            "sl" -> LANGUAGE_SLOVENIAN
            "bg" -> LANGUAGE_BULGARIAN
            "eo" -> LANGUAGE_ESPERANTO
            else -> LANGUAGE_ENGLISH_QWERTY
        }
    }

    var wasDictionaryLanguageProposed: Boolean
        get() = prefs.getBoolean(DICTIONARY_LANGUAGE_PROPOSED, false)
        set(wasDictionaryLanguageProposed) = prefs.edit().putBoolean(DICTIONARY_LANGUAGE_PROPOSED, wasDictionaryLanguageProposed).apply()

    var showSuggestions: Boolean
        get() = prefs.getBoolean(SHOW_SUGGESTIONS, false)
        set(showSuggestions) = prefs.edit().putBoolean(SHOW_SUGGESTIONS, showSuggestions).apply()

    var autoCorrectOnSpace: Boolean
        get() = prefs.getBoolean(AUTO_CORRECT_ON_SPACE, false)
        set(autoCorrectOnSpace) = prefs.edit().putBoolean(AUTO_CORRECT_ON_SPACE, autoCorrectOnSpace).apply()

    var recentlyUsedEmojis: List<String>
        get() = prefs.getString(RECENTLY_USED_EMOJIS, "\uD83D\uDC30")!!.split("|").filter { it.isNotEmpty() }
        set(recentlyUsedEmojis) = prefs.edit().putString(
            RECENTLY_USED_EMOJIS, recentlyUsedEmojis.joinToString("|")
        ).apply()

    fun addRecentEmoji(emoji: String) {
        val recentEmojis = recentlyUsedEmojis.toMutableList()
        recentEmojis.remove(emoji)
        recentEmojis.add(0, emoji)
        recentlyUsedEmojis = recentEmojis.take(RECENT_EMOJIS_LIMIT)
    }
}
