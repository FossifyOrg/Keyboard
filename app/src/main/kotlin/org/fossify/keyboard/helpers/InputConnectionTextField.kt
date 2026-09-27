package org.fossify.keyboard.helpers

import android.view.inputmethod.InputConnection
import org.fossify.keyboard.suggestions.TextField

/** Lets word suggestions read and edit the text field of an [InputConnection]. */
class InputConnectionTextField(private val connection: InputConnection) : TextField {
    override fun textBeforeCursor(length: Int): CharSequence? = connection.getTextBeforeCursor(length, 0)

    override fun textAfterCursor(length: Int): CharSequence? = connection.getTextAfterCursor(length, 0)

    override fun replaceBeforeCursor(length: Int, text: String) {
        connection.beginBatchEdit()
        connection.deleteSurroundingText(length, 0)
        connection.commitText(text, 1)
        connection.endBatchEdit()
    }
}
