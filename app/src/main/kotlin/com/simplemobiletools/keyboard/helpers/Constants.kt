package com.simplemobiletools.keyboard.helpers


enum class ShiftState {
    OFF,
    ON_ONE_CHAR,
    ON_PERMANENT;
}

// limit the count of alternative characters that show up at long pressing a key
const val MAX_KEYS_PER_MINI_ROW = 9

// shared prefs
const val VIBRATE_ON_KEYPRESS = "vibrate_on_keypress"
const val SHOW_POPUP_ON_KEYPRESS = "show_popup_on_keypress"
const val SHOW_KEY_BORDERS = "show_key_borders"
const val SENTENCES_CAPITALIZATION = "sentences_capitalization"
const val LAST_EXPORTED_CLIPS_FOLDER = "last_exported_clips_folder"
const val KEYBOARD_LANGUAGE = "keyboard_language"
const val HEIGHT_PERCENTAGE = "height_percentage"
const val SHOW_CLIPBOARD_CONTENT = "show_clipboard_content"
const val SHOW_NUMBERS_ROW = "show_numbers_row"

// differentiate current and pinned clips at the keyboards' Clipboard section
const val ITEM_SECTION_LABEL = 0
const val ITEM_CLIP = 1

const val LANGUAGE_ENGLISH_QWERTY = 0
const val LANGUAGE_RUSSIAN = 1
const val LANGUAGE_FRENCH_AZERTY = 2
const val LANGUAGE_ENGLISH_QWERTZ = 3
const val LANGUAGE_SPANISH = 4
const val LANGUAGE_GERMAN = 5
const val LANGUAGE_ENGLISH_DVORAK = 6
const val LANGUAGE_ROMANIAN = 7
const val LANGUAGE_SLOVENIAN = 8
const val LANGUAGE_BULGARIAN = 9
const val LANGUAGE_TURKISH_Q = 10
const val LANGUAGE_LITHUANIAN = 11
const val LANGUAGE_BENGALI = 12
const val LANGUAGE_GREEK = 13
const val LANGUAGE_NORWEGIAN = 14
const val LANGUAGE_SWEDISH = 15
const val LANGUAGE_DANISH = 16
const val LANGUAGE_FRENCH_BEPO = 17
const val LANGUAGE_VIETNAMESE_TELEX = 18
const val LANGUAGE_POLISH = 19
const val LANGUAGE_UKRAINIAN = 20

const val LANGUAGE_ENGLISH_QWERTY_SELECTED = "language_english_qwerty_selected"
const val LANGUAGE_RUSSIAN_SELECTED = "language_russian_selected"
const val LANGUAGE_FRENCH_AZERTY_SELECTED = "language_french_azerty_selected"
const val LANGUAGE_ENGLISH_QWERTZ_SELECTED = "language__selected"
const val LANGUAGE_SPANISH_SELECTED = "language_spanish_selected"
const val LANGUAGE_GERMAN_SELECTED = "language_german_selected"
const val LANGUAGE_ENGLISH_DVORAK_SELECTED = "language_english_dvorak_selected"
const val LANGUAGE_ROMANIAN_SELECTED = "language_romanian_selected"
const val LANGUAGE_SLOVENIAN_SELECTED = "language_slovenian_selected"
const val LANGUAGE_BULGARIAN_SELECTED = "language_bulgarian_selected"
const val LANGUAGE_TURKISH_Q_SELECTED = "language_turkish_q_selected"
const val LANGUAGE_LITHUANIAN_SELECTED = "language_lithuanian_selected"
const val LANGUAGE_BENGALI_SELECTED = "language_bengali_selected"
const val LANGUAGE_GREEK_SELECTED = "language_greek_selected"
const val LANGUAGE_NORWEGIAN_SELECTED = "language_norwegian_selected"
const val LANGUAGE_SWEDISH_SELECTED = "language_swedish_selected"
const val LANGUAGE_DANISH_SELECTED = "language_danish_selected"
const val LANGUAGE_FRENCH_BEPO_SELECTED = "language_french_bepo_selected"
const val LANGUAGE_VIETNAMESE_TELEX_SELECTED = "language_vietnamese_selected"
const val LANGUAGE_POLISH_SELECTED = "language_polish_selected"
const val LANGUAGE_UKRAINIAN_SELECTED = "language_ukrainian_selected"

// keyboard height percentage options
const val KEYBOARD_HEIGHT_70_PERCENT = 70
const val KEYBOARD_HEIGHT_80_PERCENT = 80
const val KEYBOARD_HEIGHT_90_PERCENT = 90
const val KEYBOARD_HEIGHT_100_PERCENT = 100
const val KEYBOARD_HEIGHT_120_PERCENT = 120
const val KEYBOARD_HEIGHT_140_PERCENT = 140
const val KEYBOARD_HEIGHT_160_PERCENT = 160

const val EMOJI_SPEC_FILE_PATH = "media/emoji_spec.txt"
const val LANGUAGE_VN_TELEX = "language/extension.json"
