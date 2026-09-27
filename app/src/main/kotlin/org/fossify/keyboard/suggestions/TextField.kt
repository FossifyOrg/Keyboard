package org.fossify.keyboard.suggestions

/** The text field being typed into, as far as suggestions need it. The keyboard backs it with an InputConnection. */
interface TextField {
    fun textBeforeCursor(length: Int): CharSequence?

    fun textAfterCursor(length: Int): CharSequence?

    /** Replaces the [length] chars before the cursor with [text] in a single edit. */
    fun replaceBeforeCursor(length: Int, text: String)
}
