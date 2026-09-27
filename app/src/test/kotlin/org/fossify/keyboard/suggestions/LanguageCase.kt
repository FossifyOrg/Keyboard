package org.fossify.keyboard.suggestions

/** A row of keys of a layout, each [width] percent of the screen wide, after [start] percent of other keys. */
class KeyRow(val keys: String, val width: Float = 10f, val start: Float = 0f)

/**
 * A language the engine is evaluated and tuned on: its dictionary and [LanguageRules], the [geometry] of the layout its
 * typos are made on, its typo datasets in `typos/<locale>/` and the everyday corrections tuning may not lose.
 */
class LanguageCase(
    val locale: String,
    val geometry: KeyboardGeometry,
    /** Whether a dictionary word is used to generate typos and completions. */
    val isSourceWord: (String) -> Boolean,
    /** How many words the dictionary has. */
    val sizes: IntRange,
    /** The most frequent word of the language. */
    val commonestWord: String,
    /** Typos that autocorrect must fix, and their fix. */
    val mustCorrect: List<Pair<String, String>> = emptyList(),
    /** Typos whose best suggestion must be the given word. */
    val mustSuggest: List<Pair<String, String>> = emptyList(),
    /** Spellings the dictionary must have. */
    val expectedWords: List<String> = emptyList(),
    /** Typos and noise the dictionary must not have. */
    val absentWords: List<String> = emptyList(),
    /** Words that are valid but must never be suggested. */
    val offensiveWords: List<String> = emptyList(),
    /** The largest share of each typo dataset that autocorrect may change into a wrong word. */
    val maxAutocorrectWrong: Float = 0.05f,
) {
    val rules: LanguageRules
        get() = LanguageRules.forLocale(locale)

    val entries: List<DictionaryEntry>
        get() = TestDictionary.entries(locale)

    val trie: ArrayTrie
        get() = TestDictionary.trie(locale)

    val evaluation: Evaluation
        get() = Evaluation.of(this)

    /** An engine for the language, with its tuned weights unless others are given. */
    fun engine(weights: EngineWeights = rules.weights) =
        SuggestionEngine(trie, geometry, rules.copy(weights = weights))

    fun resource(name: String) = "/typos/$locale/$name"

    fun hasResource(name: String) = LanguageCase::class.java.getResource(resource(name)) != null

    override fun toString() = locale

    companion object {
        val ENGLISH = LanguageCase(
            locale = "en_US",
            geometry = KeyboardGeometry.QWERTY,
            isSourceWord = { word -> word.all { it in 'a'..'z' } },
            mustCorrect = listOf(
                "teh" to "the", "recieve" to "receive", "becuase" to "because", "i" to "I", "monday" to "Monday",
                "dont" to "don't", "im" to "I'm", "wuick" to "quick", "broen" to "brown",
            ),
            mustSuggest = listOf(
                "yhe" to "the", "wrod" to "word", "adress" to "address", "thier" to "their", "tomorow" to "tomorrow",
                "definately" to "definitely", "goign" to "going", "knwo" to "know", "hapy" to "happy",
                "cafe" to "café",
            ),
            sizes = 50_000..90_000,
            commonestWord = "the",
            expectedWords = listOf("the", "I", "don't", "Monday", "TV", "iPhone", "café"),
            absentWords = listOf("teh", "dont", "tj", "dog's"),
            offensiveWords = listOf("shit"),
        )

        val SPANISH = LanguageCase(
            locale = "es",
            geometry = layout(KeyRow("qwertyuiop"), KeyRow("asdfghjklñ"), KeyRow("zxcvbnm", start = SHIFT)),
            isSourceWord = ::isLetterWord,
            sizes = 90_000..115_000,
            commonestWord = "de",
            mustCorrect = listOf(
                "tambien" to "también", "aora" to "ahora", "despues" to "después", "manana" to "mañana",
                "qeu" to "que", "ano" to "año", "ademas" to "además",
            ),
            mustSuggest = listOf(
                "graciad" to "gracias", "hoal" to "hola", "cpmo" to "como", "porqie" to "porque",
                "exelente" to "excelente", "nesecito" to "necesito", "esta" to "esta", "está" to "está",
            ),
            expectedWords = listOf("año", "está", "esta", "sí", "si", "niño", "España", "también", "qué", "que"),
            absentWords = listOf("ano", "nesecito", "tambien", "aora"),
            offensiveWords = listOf("mierda", "puta"),
            // Spanish misspellings are often phonetic (haiga, llendo, aser), which a model of keyboard slips can't
            // tell from slips of other words
            maxAutocorrectWrong = 0.06f,
        )

        /** Every language with a dictionary, English first. */
        val ALL = listOf(ENGLISH, SPANISH)

        fun of(locale: String) = ALL.first { it.locale == locale }

        /** Words of letters that fold into the alphabet, whatever their case and accents. */
        fun isLetterWord(word: String) = word.all { it.isLetter() && Alphabet.isWordChar(it) }

        /**
         * The geometry of a layout like its XML places the keys: each row a string of keys [KeyRow.width] percent of
         * the screen wide, after [KeyRow.start] percent of other keys such as shift.
         */
        fun layout(vararg rows: KeyRow): KeyboardGeometry {
            val bounds = rows.flatMapIndexed { row, keys ->
                keys.keys.mapIndexed { i, char ->
                    val x = ((keys.start + i * keys.width) * SCALE).toInt()
                    KeyboardGeometry.KeyBounds(char, x, row * ROW_HEIGHT, (keys.width * SCALE).toInt(), ROW_HEIGHT)
                }
            }
            return requireNotNull(KeyboardGeometry.fromKeyBounds(bounds))
        }

        /** Where the letters of the bottom row start, after the shift key. */
        private const val SHIFT = 15f
        private const val SCALE = 100
        private const val ROW_HEIGHT = 1500
    }
}
