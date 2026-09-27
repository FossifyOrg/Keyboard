package org.fossify.keyboard.suggestions

/**
 * Word suggestions, autocorrect and learning while typing into one text field: finds the word before the cursor, lays
 * out suggestions for it, replaces it with the one picked and corrects or learns it when a separator is typed. It
 * reads the text again for every change instead of tracking the cursor, as apps may change the text too and selection
 * updates can arrive late.
 */
class SuggestionSession {
    /** The engine of the dictionary for the keyboard language, or null while none is loaded. */
    var engine: SuggestionEngine? = null

    /** Whether the settings and the text field allow suggestions. */
    var isEnabled = false

    /** Whether the settings and the text field allow autocorrect. It needs suggestions to be enabled too. */
    var isAutocorrectEnabled = false

    /** Whether the settings and the text field allow learning words. It needs suggestions to be enabled too. */
    var isLearningEnabled = false

    /** Called with a word to learn, or a learned word, and how many more times the user confirmed it. */
    var onLearn: ((word: String, uses: Int) -> Unit)? = null

    var chips: List<SuggestionChip?> = emptyList()
        private set

    /** The word [chips] were made for. */
    private var word: String? = null

    /** Words whose correction was undone or which were kept by tapping them. They aren't corrected again. */
    private val reverted = HashSet<String>()

    private var lastCorrection: LastCorrection? = null

    /** How often unknown words were confirmed, by folded key. It isn't reset between text fields. */
    private val unknownWordUses = object : LinkedHashMap<String, Int>(MAX_UNKNOWN_WORDS, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>) = size > MAX_UNKNOWN_WORDS
    }

    /** Updates [chips] for the word before the cursor. Returns true if they changed. */
    fun refresh(field: TextField): Boolean {
        val engine = engine
        val text = if (isEnabled && engine != null) readText(field) else null
        if (text == null || lastCorrection?.matches(text) != true) lastCorrection = null

        val current = text?.let { CurrentWord.find(it.before, it.after) }
        word = current?.word
        if (engine == null || current == null) return show(emptyList())

        val suggestions = engine.suggest(current.word)
        val correction = if (isAutocorrectEnabled) {
            AutocorrectPolicy.correctionFor(current.token, suggestions, reverted, isSticky(current.word))
        } else {
            null
        }

        return show(SuggestionStrip.arrange(suggestions, correction))
    }

    /**
     * Called before [char] is typed. If it's a separator, the word before the cursor is finished: it's corrected if
     * autocorrect is sure about it, or else kept and counted towards learning it. Returns true if [char] was typed
     * along with a correction. A new line is never typed here, as the keyboard sends it as a key press.
     */
    fun onCharTyped(field: TextField, char: Char): Boolean {
        lastCorrection = null
        return finishWord(field, char)
    }

    private fun finishWord(field: TextField, char: Char): Boolean {
        val engine = engine ?: return false
        if (!isEnabled || !isSeparator(char)) return false

        val text = readText(field) ?: return false
        val current = CurrentWord.find(text.before, text.after) ?: return false

        // A single letter before a period is an abbreviation, like the i of i.e.
        if (char == PERIOD && current.word.length == 1) return false

        val suggestions = engine.suggest(current.word)
        val correction = AutocorrectPolicy.correctionFor(current.token, suggestions, reverted, isSticky(current.word))
        return when {
            correction == null -> {
                if (AutocorrectPolicy.isCorrectable(current.token, current.word)) confirm(current.word, 1)
                false
            }

            isAutocorrectEnabled -> applyCorrection(field, text, current, correction, char)
            else -> false
        }
    }

    private fun applyCorrection(
        field: TextField,
        text: TextAroundCursor,
        current: CurrentWord,
        correction: ScoredWord,
        char: Char,
    ): Boolean {
        val separator = char.toString()
        val typesSeparator = char != NEW_LINE
        val replacement = if (typesSeparator) correction.word + separator else correction.word
        field.replaceBeforeCursor(current.word.length, replacement)
        lastCorrection = LastCorrection(
            typed = current.word,
            corrected = correction.word,
            separator = separator,
            before = (text.before.dropLast(current.word.length).toString() + correction.word + separator)
                .takeLast(CurrentWord.CONTEXT_LENGTH),
            after = text.after.toString()
        )

        return typesSeparator
    }

    private fun isSticky(word: String) = (engine?.userWordUses(word) ?: 0) >= EngineConstants.STICKY_USES

    /**
     * Counts [uses] confirmations of a word missing from the dictionary: it's learned once it has
     * [EngineConstants.LEARN_AFTER_USES] of them, and a learned word keeps counting them towards sticking. Words
     * autocorrect would fix are typos rather than new words, so they're only confirmed when the user keeps them anyway.
     */
    private fun confirm(word: String, uses: Int) {
        val engine = engine
        val key = Alphabet.fold(word)
        when {
            !isLearningEnabled || engine == null || key == null || engine.isWord(word) -> {}
            engine.userWordUses(word) > 0 -> onLearn?.invoke(word, uses)
            else -> {
                val total = (unknownWordUses[key] ?: 0) + uses
                if (total < EngineConstants.LEARN_AFTER_USES) {
                    unknownWordUses[key] = total
                } else {
                    unknownWordUses.remove(key)
                    onLearn?.invoke(word, total)
                }
            }
        }
    }

    /**
     * Puts back the word autocorrect replaced, without the separator typed after it, if nothing was typed and the
     * cursor didn't move since. The word isn't corrected again in this text field, and the undo confirms it once.
     * Returns true if it did.
     */
    fun undoCorrection(field: TextField): Boolean {
        val correction = lastCorrection ?: return false
        lastCorrection = null
        val text = readText(field)
        if (text == null || !correction.matches(text)) return false

        field.replaceBeforeCursor(correction.corrected.length + correction.separator.length, correction.typed)
        reverted += correction.typed
        confirm(correction.typed, 1)
        return true
    }

    /**
     * Replaces the word before the cursor with the chip at [index], followed by a space. A typed word kept this way
     * isn't corrected again in this text field, and is confirmed twice: it was both kept and picked. Returns false if
     * the chip is empty or the word changed since the chips were made.
     */
    fun pick(field: TextField, index: Int): Boolean {
        val chip = chips.getOrNull(index) ?: return false
        val current = readText(field)?.let { CurrentWord.find(it.before, it.after) }
        if (current == null || current.word != word) return false

        field.replaceBeforeCursor(current.word.length, chip.word + SPACE)
        if (chip.isTyped) {
            reverted += chip.word
            confirm(chip.word, 2)
        }
        lastCorrection = null
        word = null
        show(emptyList())
        return true
    }

    /** Starts over for a new text field. Returns true if suggestions were shown. */
    fun reset(): Boolean {
        word = null
        lastCorrection = null
        reverted.clear()
        return show(emptyList())
    }

    private fun show(newChips: List<SuggestionChip?>): Boolean {
        if (newChips == chips) return false
        chips = newChips
        return true
    }

    private class TextAroundCursor(val before: CharSequence, val after: CharSequence)

    /**
     * A word autocorrect replaced, with the text expected around the cursor as long as nothing was typed and the
     * cursor didn't move since.
     */
    private class LastCorrection(
        val typed: String,
        val corrected: String,
        val separator: String,
        private val before: String,
        private val after: String,
    ) {
        fun matches(text: TextAroundCursor) = text.before.endsWith(before) && text.after.toString() == after
    }

    private companion object {
        const val SPACE = " "
        const val SEPARATORS = " .,!?;:)"
        const val PERIOD = '.'
        const val NEW_LINE = '\n'

        /** How much text after the cursor is compared to tell whether the cursor moved. */
        const val AFTER_LENGTH = 16

        const val MAX_UNKNOWN_WORDS = 256
        const val LOAD_FACTOR = 0.75f

        fun readText(field: TextField): TextAroundCursor? {
            val before = field.textBeforeCursor(CurrentWord.CONTEXT_LENGTH) ?: return null
            val after = field.textAfterCursor(AFTER_LENGTH) ?: ""
            return TextAroundCursor(before, after)
        }

        fun isSeparator(char: Char) = char in SEPARATORS || char == NEW_LINE
    }
}
