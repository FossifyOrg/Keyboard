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
    /** The largest share of valid words missing from the dictionary that autocorrect may change. */
    val maxUnknownChanged: Float = 0.1f,
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

        val PORTUGUESE = LanguageCase(
            locale = "pt_BR",
            geometry = layout(KeyRow("qwertyuiop"), KeyRow("asdfghjklç"), KeyRow("zxcvbnm", start = SHIFT)),
            isSourceWord = ::isLetterWord,
            sizes = 90_000..115_000,
            commonestWord = "de",
            mustCorrect = listOf(
                "nao" to "não", "voce" to "você", "tambem" to "também", "entao" to "então", "ha" to "há",
                "coracao" to "coração", "ate" to "até",
            ),
            mustSuggest = listOf(
                "obrigafo" to "obrigado", "qur" to "que", "oara" to "para", "exelente" to "excelente", "e" to "e",
                "é" to "é", "esta" to "esta", "está" to "está",
            ),
            expectedWords = listOf("não", "você", "é", "e", "também", "coração", "segunda-feira", "está", "esta"),
            absentWords = listOf("nao", "voce", "tambem", "ate"),
            offensiveWords = listOf("porra", "merda"),
        )

        val GERMAN = LanguageCase(
            locale = "de_DE",
            geometry = layout(
                KeyRow("qwertzuiopü", GERMAN_KEY),
                KeyRow("asdfghjklöä", GERMAN_KEY),
                KeyRow("yxcvbnm", start = SHIFT),
            ),
            isSourceWord = ::isLetterWord,
            sizes = 115_000..145_000,
            commonestWord = "der",
            mustCorrect = listOf(
                "fur" to "für", "uber" to "über", "strasse" to "Straße", "haus" to "Haus", "mussen" to "müssen",
                "konnen" to "können", "Madchen" to "Mädchen",
            ),
            mustSuggest = listOf(
                "danle" to "danke", "nicjt" to "nicht", "vielleichr" to "vielleicht", "schon" to "schon",
                "schön" to "schön", "essen" to "essen", "Essen" to "Essen",
            ),
            expectedWords = listOf("Straße", "für", "Haus", "essen", "Bär", "bar", "E-Mail", "Mädchen", "groß"),
            absentWords = listOf("fur", "uber", "strasse", "Madchen"),
            offensiveWords = listOf("Arschloch", "scheiße"),
            // German capitalizes nouns like names, so a name looks like a capitalized typo of a short word at the
            // start of a sentence (Rewe, Rede)
            maxUnknownChanged = 0.15f,
        )

        val FRENCH = LanguageCase(
            locale = "fr_FR",
            geometry = layout(KeyRow("azertyuiop"), KeyRow("qsdfghjklm"), KeyRow("wxcvbn'", start = SHIFT)),
            isSourceWord = ::isLetterWord,
            sizes = 90_000..115_000,
            commonestWord = "de",
            mustCorrect = listOf(
                "etre" to "être", "tres" to "très", "deja" to "déjà", "coeur" to "cœur", "cest" to "c'est",
                "jai" to "j'ai", "francais" to "français",
            ),
            mustSuggest = listOf(
                "bonjoue" to "bonjour", "mervi" to "merci", "pourqoi" to "pourquoi", "a" to "a", "à" to "à",
                "ou" to "ou", "où" to "où",
            ),
            expectedWords = listOf(
                "être", "cœur", "à", "a", "où", "ou", "aujourd'hui", "c'est", "français", "peut-être",
            ),
            absentWords = listOf("etre", "tres", "qu"),
            offensiveWords = listOf("putain", "merde"),
        )

        /** Every language with a dictionary, English first. */
        val ALL = listOf(ENGLISH, SPANISH, PORTUGUESE, GERMAN, FRENCH)

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

        /** The width of the keys of the German layout, which has eleven keys in its upper rows. */
        private const val GERMAN_KEY = 9.05f
        private const val SCALE = 100
        private const val ROW_HEIGHT = 1500
    }
}
