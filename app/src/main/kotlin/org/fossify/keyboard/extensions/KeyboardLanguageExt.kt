package org.fossify.keyboard.extensions

import android.annotation.SuppressLint
import android.content.Context
import org.fossify.commons.models.RadioItem
import org.fossify.keyboard.R
import org.fossify.keyboard.helpers.LANGUAGE_ARABIC
import org.fossify.keyboard.helpers.LANGUAGE_BELARUSIAN_CYRL
import org.fossify.keyboard.helpers.LANGUAGE_BELARUSIAN_LATN
import org.fossify.keyboard.helpers.LANGUAGE_BENGALI
import org.fossify.keyboard.helpers.LANGUAGE_BULGARIAN
import org.fossify.keyboard.helpers.LANGUAGE_CENTRAL_KURDISH
import org.fossify.keyboard.helpers.LANGUAGE_CHUVASH
import org.fossify.keyboard.helpers.LANGUAGE_CZECH_QWERTY
import org.fossify.keyboard.helpers.LANGUAGE_CZECH_QWERTZ
import org.fossify.keyboard.helpers.LANGUAGE_DANISH
import org.fossify.keyboard.helpers.LANGUAGE_DUTCH
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_ASSET
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_COLEMAK
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_COLEMAKDH
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_DVORAK
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_NIRO
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_QWERTZ
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_SOUL
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_WORKMAN
import org.fossify.keyboard.helpers.LANGUAGE_ESPERANTO
import org.fossify.keyboard.helpers.LANGUAGE_FRENCH_AZERTY
import org.fossify.keyboard.helpers.LANGUAGE_FRENCH_BEPO
import org.fossify.keyboard.helpers.LANGUAGE_GERMAN
import org.fossify.keyboard.helpers.LANGUAGE_GERMAN_QWERTZ
import org.fossify.keyboard.helpers.LANGUAGE_GREEK
import org.fossify.keyboard.helpers.LANGUAGE_HEBREW
import org.fossify.keyboard.helpers.LANGUAGE_ITALIAN
import org.fossify.keyboard.helpers.LANGUAGE_KABYLE_AZERTY
import org.fossify.keyboard.helpers.LANGUAGE_LATVIAN
import org.fossify.keyboard.helpers.LANGUAGE_LITHUANIAN
import org.fossify.keyboard.helpers.LANGUAGE_NORWEGIAN
import org.fossify.keyboard.helpers.LANGUAGE_POLISH
import org.fossify.keyboard.helpers.LANGUAGE_PORTUGUESE
import org.fossify.keyboard.helpers.LANGUAGE_PORTUGUESE_HCESAR
import org.fossify.keyboard.helpers.LANGUAGE_ROMANIAN
import org.fossify.keyboard.helpers.LANGUAGE_RUSSIAN
import org.fossify.keyboard.helpers.LANGUAGE_SLOVENIAN
import org.fossify.keyboard.helpers.LANGUAGE_SPANISH
import org.fossify.keyboard.helpers.LANGUAGE_SWEDISH
import org.fossify.keyboard.helpers.LANGUAGE_TURKISH
import org.fossify.keyboard.helpers.LANGUAGE_TURKISH_Q
import org.fossify.keyboard.helpers.LANGUAGE_UKRAINIAN
import org.fossify.keyboard.helpers.LANGUAGE_VIETNAMESE_TELEX

fun Context.getSelectedLanguagesSorted(): List<Int> {
    return config.selectedLanguages
        .map { it to getKeyboardLanguageText(it) }
        .sortedBy { it.second }
        .map { it.first }
}

fun Context.getKeyboardLanguagesRadioItems(): ArrayList<RadioItem> {
    return getSelectedLanguagesSorted()
        .map { RadioItem(it, getKeyboardLanguageText(it)) }
        .toMutableList() as ArrayList<RadioItem>
}

@Suppress("CyclomaticComplexMethod")
fun Context.getKeyboardLanguageText(language: Int): String {
    return when (language) {
        LANGUAGE_ARABIC -> getString(R.string.translation_arabic)
        LANGUAGE_BELARUSIAN_CYRL -> "${getString(R.string.translation_belarusian)} (Cyrillic)"
        LANGUAGE_BELARUSIAN_LATN -> "${getString(R.string.translation_belarusian)} (Latin)"
        LANGUAGE_BENGALI -> getString(R.string.translation_bengali)
        LANGUAGE_BULGARIAN -> getString(R.string.translation_bulgarian)
        LANGUAGE_CENTRAL_KURDISH -> getString(R.string.translation_central_kurdish)
        LANGUAGE_CHUVASH -> getString(R.string.translation_chuvash)
        LANGUAGE_CZECH_QWERTY -> "${getString(R.string.translation_czech)} (QWERTY)"
        LANGUAGE_CZECH_QWERTZ -> "${getString(R.string.translation_czech)} (QWERTZ)"
        LANGUAGE_DANISH -> getString(R.string.translation_danish)
        LANGUAGE_DUTCH -> getString(R.string.translation_dutch)
        LANGUAGE_ENGLISH_ASSET -> "${getString(R.string.translation_english)} (Asset)"
        LANGUAGE_ENGLISH_COLEMAK -> "${getString(R.string.translation_english)} (Colemak)"
        LANGUAGE_ENGLISH_COLEMAKDH -> "${getString(R.string.translation_english)} (Colemak-DH)"
        LANGUAGE_ENGLISH_DVORAK -> "${getString(R.string.translation_english)} (DVORAK)"
        LANGUAGE_ENGLISH_NIRO -> "${getString(R.string.translation_english)} (Niro)"
        LANGUAGE_ENGLISH_QWERTZ -> "${getString(R.string.translation_english)} (QWERTZ)"
        LANGUAGE_ENGLISH_SOUL -> "${getString(R.string.translation_english)} (Soul)"
        LANGUAGE_ENGLISH_WORKMAN -> "${getString(R.string.translation_english)} (Workman)"
        LANGUAGE_ESPERANTO -> getString(R.string.translation_esperanto)
        LANGUAGE_FRENCH_AZERTY -> "${getString(R.string.translation_french)} (AZERTY)"
        LANGUAGE_FRENCH_BEPO -> "${getString(R.string.translation_french)} (BEPO)"
        LANGUAGE_GERMAN -> getString(R.string.translation_german)
        LANGUAGE_GERMAN_QWERTZ -> "${getString(R.string.translation_german)} (QWERTZ)"
        LANGUAGE_GREEK -> getString(R.string.translation_greek)
        LANGUAGE_HEBREW -> getString(R.string.translation_hebrew)
        LANGUAGE_ITALIAN -> getString(R.string.translation_italian)
        LANGUAGE_KABYLE_AZERTY -> "${getString(R.string.translation_kabyle)} (AZERTY)"
        LANGUAGE_LATVIAN -> getString(R.string.translation_latvian)
        LANGUAGE_LITHUANIAN -> getString(R.string.translation_lithuanian)
        LANGUAGE_NORWEGIAN -> getString(R.string.translation_norwegian)
        LANGUAGE_POLISH -> getString(R.string.translation_polish)
        LANGUAGE_PORTUGUESE -> getString(R.string.translation_portuguese)
        LANGUAGE_PORTUGUESE_HCESAR -> "${getString(R.string.translation_portuguese)} (HCESAR)"
        LANGUAGE_ROMANIAN -> getString(R.string.translation_romanian)
        LANGUAGE_RUSSIAN -> getString(R.string.translation_russian)
        LANGUAGE_SLOVENIAN -> getString(R.string.translation_slovenian)
        LANGUAGE_SPANISH -> getString(R.string.translation_spanish)
        LANGUAGE_SWEDISH -> getString(R.string.translation_swedish)
        LANGUAGE_TURKISH -> getString(R.string.translation_turkish)
        LANGUAGE_TURKISH_Q -> "${getString(R.string.translation_turkish)} (Q)"
        LANGUAGE_UKRAINIAN -> getString(R.string.translation_ukrainian)
        LANGUAGE_VIETNAMESE_TELEX -> "${getString(R.string.translation_vietnamese)} (Telex)"
        else -> "${getString(R.string.translation_english)} (QWERTY)"
    }
}
