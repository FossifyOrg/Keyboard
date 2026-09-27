package org.fossify.keyboard.suggestions

/** A text field for tests, with the cursor where `|` is in [initial]. */
class FakeTextField(initial: String) : TextField {
    private val text = StringBuilder(initial.replace("|", ""))
    var cursor = initial.indexOf('|').let { if (it == -1) text.length else it }
    var edits = 0
        private set

    override fun textBeforeCursor(length: Int): CharSequence = text.substring(maxOf(0, cursor - length), cursor)

    override fun textAfterCursor(length: Int): CharSequence =
        text.substring(cursor, minOf(text.length, cursor + length))

    override fun replaceBeforeCursor(length: Int, text: String) {
        this.text.replace(cursor - length, cursor, text)
        cursor += text.length - length
        edits++
    }

    fun type(chars: String) {
        text.insert(cursor, chars)
        cursor += chars.length
    }

    /** The text with `|` at the cursor. */
    override fun toString() = StringBuilder(text).insert(cursor, '|').toString()
}
