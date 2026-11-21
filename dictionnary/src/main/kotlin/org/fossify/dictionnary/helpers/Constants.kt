package org.fossify.dictionnary.helpers

// authority of the content provider used to share the downloaded dictionary files
const val AUTHORITY = "org.fossify.dictionnary.provider"

// signature level permission, shared with the keyboard app when both are signed with the same key
const val ACCESS_PERMISSION = "org.fossify.dictionnary.ACCESS_DICTIONARIES"

// base URL of the raw word frequency lists (OpenSubtitles 2018 by HermitDave/FrequencyWords,
// one "<code>_50k.txt" file per language with "word count" per line). Only this app is
// allowed to reach the network; the lists are converted on the fly to the JSON format the
// keyboard expects ("<lang>.json", wordfreq style: [ [word, log10(frequency)], ... ]).
const val DICTIONARY_BASE_URL =
    "https://raw.githubusercontent.com/HermitDave/FrequencyWords/master/content/2018"

// maps our dictionary codes ("<lang>.json") to the language folder of the source lists
val DICTIONARY_SOURCE_CODES = mapOf(
    "en_US" to "en",
    "fr_FR" to "fr",
    "de_DE" to "de",
    "es_ES" to "es",
    "it_IT" to "it",
    "pt_PT" to "pt",
    "nl_NL" to "nl",
    "pl_PL" to "pl",
    "sv_SE" to "sv",
    "da_DK" to "da",
    "nb_NO" to "no",
    "ru_RU" to "ru",
    "uk_UA" to "uk",
    "cs_CZ" to "cs",
    "tr_TR" to "tr",
    "el_GR" to "el",
    "he_IL" to "he",
    "ar_AR" to "ar",
    "bn_BD" to "bn",
    "lv_LV" to "lv",
    "lt_LT" to "lt",
    "ro_RO" to "ro",
    "sl_SI" to "sl",
    "bg_BG" to "bg",
    "eo" to "eo"
)

// languages this app knows how to download and serve, the file name is "<lang>.json"
val AVAILABLE_DICTIONARIES = DICTIONARY_SOURCE_CODES.keys.toList()

// raw "word count" list served for a dictionary language, null when the language is unknown
fun dictionarySourceUrl(lang: String): String? {
    val code = DICTIONARY_SOURCE_CODES[lang] ?: return null
    return "$DICTIONARY_BASE_URL/$code/${code}_50k.txt"
}

// provider paths
const val PATH_DICTIONARIES = "dictionaries"
const val PATH_FILES = "files"

// provider "call" methods
const val METHOD_DOWNLOAD = "download"
const val METHOD_LIST = "list"

// activity action used by the keyboard app to ask for a download and get the result back
const val ACTION_DOWNLOAD_DICTIONARY = "org.fossify.dictionnary.action.DOWNLOAD_DICTIONARY"

// intent extras
const val EXTRA_LANG = "lang"
const val EXTRA_STARTED = "started"
const val EXTRA_LANGUAGES = "languages"
const val EXTRA_CALLING_PACKAGE = "calling_package"
const val EXTRA_RESULT_URI = "result_uri"
const val EXTRA_ERROR = "error"

// cursor columns returned by query()
const val COLUMN_ID = "_id"
const val COLUMN_LANG = "lang"
const val COLUMN_NAME = "name"
const val COLUMN_URI = "uri"
const val COLUMN_SIZE = "size"
const val COLUMN_DOWNLOADED = "downloaded"

// uri matcher codes
const val CODE_DICTIONARIES = 1
const val CODE_DICTIONARY = 2
const val CODE_FILE = 3

fun dictionaryFileUri(lang: String): String =
    "content://$AUTHORITY/$PATH_FILES/$PATH_DICTIONARIES/$lang.json"
