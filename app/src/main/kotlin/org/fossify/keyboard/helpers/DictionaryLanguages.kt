package org.fossify.keyboard.helpers

/**
 * Maps a keyboard language (see LANGUAGE_* constants) to the dictionary language code
 * ("<lang>.json") served by the external Dictionnary app. Returns null when no dictionary
 * exists for that keyboard language.
 */
fun dictionaryLangForKeyboardLanguage(language: Int): String? = when (language) {
    LANGUAGE_ENGLISH_QWERTY,
    LANGUAGE_ENGLISH_QWERTZ,
    LANGUAGE_ENGLISH_DVORAK,
    LANGUAGE_ENGLISH_ASSET,
    LANGUAGE_ENGLISH_COLEMAK,
    LANGUAGE_ENGLISH_COLEMAKDH,
    LANGUAGE_ENGLISH_NIRO,
    LANGUAGE_ENGLISH_SOUL,
    LANGUAGE_ENGLISH_WORKMAN -> "en_US"

    LANGUAGE_FRENCH_AZERTY, LANGUAGE_FRENCH_BEPO, LANGUAGE_KABYLE_AZERTY -> "fr_FR"
    LANGUAGE_GERMAN, LANGUAGE_GERMAN_QWERTZ -> "de_DE"
    LANGUAGE_SPANISH -> "es_ES"
    LANGUAGE_ITALIAN -> "it_IT"
    LANGUAGE_PORTUGUESE, LANGUAGE_PORTUGUESE_HCESAR -> "pt_PT"
    LANGUAGE_DUTCH -> "nl_NL"
    LANGUAGE_POLISH -> "pl_PL"
    LANGUAGE_SWEDISH -> "sv_SE"
    LANGUAGE_DANISH -> "da_DK"
    LANGUAGE_NORWEGIAN -> "nb_NO"
    LANGUAGE_RUSSIAN -> "ru_RU"
    LANGUAGE_UKRAINIAN -> "uk_UA"
    LANGUAGE_CZECH_QWERTY, LANGUAGE_CZECH_QWERTZ -> "cs_CZ"
    LANGUAGE_TURKISH, LANGUAGE_TURKISH_Q -> "tr_TR"
    LANGUAGE_GREEK -> "el_GR"
    LANGUAGE_HEBREW -> "he_IL"
    LANGUAGE_ARABIC -> "ar_AR"
    LANGUAGE_BENGALI -> "bn_BD"
    LANGUAGE_LATVIAN -> "lv_LV"
    LANGUAGE_LITHUANIAN -> "lt_LT"
    LANGUAGE_ROMANIAN -> "ro_RO"
    LANGUAGE_SLOVENIAN -> "sl_SI"
    LANGUAGE_BULGARIAN -> "bg_BG"
    LANGUAGE_ESPERANTO -> "eo"
    else -> null
}
