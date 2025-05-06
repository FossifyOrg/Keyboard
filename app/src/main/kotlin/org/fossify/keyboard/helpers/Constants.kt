package org.fossify.keyboard.helpers


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
const val SELECTED_LANGUAGES = "selected_languages"
const val VOICE_INPUT_METHOD = "voice_input_method"
const val RECENTLY_USED_EMOJIS = "recently_used_emojis"

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
const val LANGUAGE_CHUVASH = 22
const val LANGUAGE_ESPERANTO = 23
const val LANGUAGE_HEBREW = 24
const val LANGUAGE_ARABIC = 25
const val LANGUAGE_CENTRAL_KURDISH = 26
const val LANGUAGE_BELARUSIAN_CYRL = 27
const val LANGUAGE_BELARUSIAN_LATN = 28
const val LANGUAGE_KABYLE_AZERTY = 29

// Keep this sorted
val SUPPORTED_LANGUAGES = listOf(
    LANGUAGE_ARABIC,
    LANGUAGE_BELARUSIAN_CYRL,
    LANGUAGE_BELARUSIAN_LATN,
    LANGUAGE_BENGALI,
    LANGUAGE_BULGARIAN,
    LANGUAGE_CENTRAL_KURDISH,
    LANGUAGE_CHUVASH,
    LANGUAGE_DANISH,
    LANGUAGE_ENGLISH_QWERTY,
    LANGUAGE_ENGLISH_QWERTZ,
    LANGUAGE_ENGLISH_DVORAK,
    LANGUAGE_ESPERANTO,
    LANGUAGE_FRENCH_AZERTY,
    LANGUAGE_FRENCH_BEPO,
    LANGUAGE_GERMAN,
    LANGUAGE_GREEK,
    LANGUAGE_HEBREW,
    LANGUAGE_KABYLE_AZERTY,
    LANGUAGE_LITHUANIAN,
    LANGUAGE_NORWEGIAN,
    LANGUAGE_POLISH,
    LANGUAGE_ROMANIAN,
    LANGUAGE_RUSSIAN,
    LANGUAGE_SLOVENIAN,
    LANGUAGE_SPANISH,
    LANGUAGE_SWEDISH,
    LANGUAGE_TURKISH_Q,
    LANGUAGE_UKRAINIAN,
    LANGUAGE_VIETNAMESE_TELEX
)

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
const val RECENT_EMOJIS_LIMIT = 36

// Android constant
const val INPUT_METHOD_SUBTYPE_VOICE = "voice"
