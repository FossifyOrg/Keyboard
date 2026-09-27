package org.fossify.keyboard.helpers

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.dictionaryLocale
import org.fossify.keyboard.extensions.isDeviceLocked
import org.fossify.keyboard.suggestions.KeyboardGeometry
import org.fossify.keyboard.suggestions.LanguageRules
import org.fossify.keyboard.suggestions.SuggestionChip
import org.fossify.keyboard.suggestions.SuggestionEngine
import org.fossify.keyboard.suggestions.SuggestionSession

/**
 * Word suggestions for the keyboard: loads the dictionary of the keyboard language, applies the settings and the
 * policy of the text field, shows suggestions for the word before the cursor on the strip and saves learned words.
 *
 * Suggestions are looked up on the main thread, which the node budget and the cache of the engine keep fast.
 */
class SuggestionsController(
    private val context: Context,
    private val inputConnection: () -> InputConnection?,
    private val onChipsChanged: (List<SuggestionChip?>) -> Unit,
) {
    private val session = SuggestionSession()
    private val dictionaries = DictionaryRepository(context)
    private var fieldPolicy = FieldPolicy.DISABLED
    private var geometry = KeyboardGeometry.QWERTY
    private var engine: SuggestionEngine? = null
    private var engineLocale: String? = null

    init {
        session.onLearn = { word, uses ->
            engineLocale?.let { UserWordsRepository.getInstance(context).learn(it, word, uses) }
        }
    }

    /**
     * Applies the keyboard language and the suggestion settings. The dictionary of the language is loaded in the
     * background, unless it's loaded already.
     */
    fun loadSettings() {
        val locale = context.config.keyboardLanguage.dictionaryLocale()
        if (locale != null && locale != engineLocale) {
            dictionaries.load(locale) { trie ->
                // The language may have changed while the dictionary was loading
                if (context.config.keyboardLanguage.dictionaryLocale() == locale) {
                    engine = SuggestionEngine(trie, geometry, LanguageRules.forLocale(locale))
                    engineLocale = locale
                    updateSession()
                    refresh()
                }
            }
        }

        updateSession()
        refresh()
    }

    /** Starts over for the text field described by [editorInfo], or stops suggesting if it's null. */
    fun onInputChanged(editorInfo: EditorInfo?) {
        fieldPolicy = FieldPolicy.from(editorInfo, context.isDeviceLocked)
        publish(session.reset())
        loadSettings()
    }

    /** Takes the key positions of a new keyboard. Keyboards without letters, like the symbols, are ignored. */
    fun onKeyboardChanged(keyboard: MyKeyboard) {
        val bounds = keyboard.mKeys.orEmpty().mapNotNull { key ->
            key.label.singleOrNull()?.let { KeyboardGeometry.KeyBounds(it, key.x, key.y, key.width, key.height) }
        }

        KeyboardGeometry.fromKeyBounds(bounds)?.let {
            geometry = it
            engine?.geometry = it
        }
    }

    /** Updates the suggestions for the word before the cursor. Call it after every change of the text. */
    fun refresh() {
        val connection = inputConnection() ?: return
        publish(session.refresh(InputConnectionTextField(connection)))
    }

    /** Called before a typed char is committed. Returns true if autocorrect committed it with a correction. */
    fun onCharTyped(char: Char): Boolean {
        val connection = inputConnection() ?: return false
        return session.onCharTyped(InputConnectionTextField(connection), char)
    }

    /** Undoes the last autocorrection if nothing happened since. Returns true if it did. */
    fun undoCorrection(): Boolean {
        val connection = inputConnection() ?: return false
        return session.undoCorrection(InputConnectionTextField(connection))
    }

    /** Replaces the word before the cursor with the suggestion at [index] of the strip. Returns true if it did. */
    fun pick(index: Int): Boolean {
        val connection = inputConnection() ?: return false
        val picked = session.pick(InputConnectionTextField(connection), index)
        if (picked) publish(true) else refresh()
        return picked
    }

    private fun updateSession() {
        val config = context.config
        val locale = config.keyboardLanguage.dictionaryLocale()
        session.engine = engine.takeIf { locale != null && locale == engineLocale }
        session.isEnabled = config.showWordSuggestions && fieldPolicy.suggestions
        session.isAutocorrectEnabled = session.isEnabled && config.autoCorrect && fieldPolicy.autocorrect
        session.isLearningEnabled = session.isEnabled && config.learnWords && fieldPolicy.learning

        // Learned words are only read once a text field allows suggestions, so never on a locked device
        val engine = session.engine
        if (engine != null && locale != null && session.isEnabled) {
            engine.userLexicon = UserWordsRepository.getInstance(context).lexicon(locale)
        }
    }

    private fun publish(changed: Boolean) {
        if (changed) onChipsChanged(session.chips)
    }
}
