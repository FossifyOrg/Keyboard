package org.fossify.keyboard.helpers

import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.style.SuggestionSpan
import android.util.Log
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

/**
 * Source: https://stackoverflow.com/a/39460124
 */
class OtherInputConnection(private val mTextView: AppCompatAutoCompleteTextView?) : BaseInputConnection(
    mTextView!!, true
) {
    // Keeps track of nested begin/end batch edit to ensure this connection always has a
    // balanced impact on its associated TextView.
    // A negative value means that this connection has been finished by the InputMethodManager.
    private var mBatchEditNesting = 0


    override fun getEditable(): Editable? {
        val tv = mTextView
         
        return tv?.editableText
    }



    override fun beginBatchEdit(): Boolean {
        synchronized(this) {
            if (mBatchEditNesting >= 0) {
                mTextView!!.beginBatchEdit()
                mBatchEditNesting++
                return true
            }
        }
        return false
    }

    override fun endBatchEdit(): Boolean {
        synchronized(this) {
            if (mBatchEditNesting > 0) {
                // When the connection is reset by the InputMethodManager and reportFinish
                // is called, some endBatchEdit calls may still be asynchronously received from the
                // IME. Do not take these into account, thus ensuring that this IC's final
                // contribution to mTextView's nested batch edit count is zero.
                mTextView!!.endBatchEdit()
                mBatchEditNesting--
                return true
            }
        }
        return false
    }

    //clear the meta key states means shift, alt, ctrl
    override fun clearMetaKeyStates(states: Int): Boolean {
        val content = editable ?: return false
        val kl = mTextView!!.keyListener //listen keyevents like a, enter, space
        if (kl != null) {
            try {
                kl.clearMetaKeyState(mTextView, content, states)
            } catch (e: AbstractMethodError) {
                // This is an old listener that doesn't implement the
                // new method.
            }
        }
        return true
    }

    //When a user selects a suggestion from an autocomplete or suggestion list, the input method may call commitCompletion
    override fun commitCompletion(text: CompletionInfo): Boolean {
        if (DEBUG) Log.v(
            TAG,
            "commitCompletion $text"
        )
        mTextView!!.beginBatchEdit()
        mTextView.onCommitCompletion(text)
        mTextView.endBatchEdit()
        return true
    }

    /**
    which is used to commit a correction to a previously entered text.
    This correction could be suggested by the input method or obtained through some other means.
     */
    override fun commitCorrection(correctionInfo: CorrectionInfo): Boolean {
        if (DEBUG) Log.v(
            TAG,
            "commitCorrection$correctionInfo"
        )
        mTextView!!.beginBatchEdit()
        mTextView.onCommitCorrection(correctionInfo)
        mTextView.endBatchEdit()
        return true
    }

    /* It's used to simulate the action associated with an editor action, typically triggered by pressing the "Done" or "Enter" key on the keyboard.*/
    override fun performEditorAction(actionCode: Int): Boolean {
        if (DEBUG) Log.v(
            TAG,
            "performEditorAction $actionCode"
        )
        mTextView!!.onEditorAction(actionCode)
        return true
    }

/*
    handle actions triggered from the context menu associated with the search text.
     This menu typically appears when you long-press on the search text field.
*/
    override fun performContextMenuAction(id: Int): Boolean {
        if (DEBUG) Log.v(
            TAG,
            "performContextMenuAction $id"
        )
        mTextView!!.beginBatchEdit()
        mTextView.onTextContextMenuItem(id)
        mTextView.endBatchEdit()
        return true
    }

    /*It is used to retrieve information about the currently extracted text
    * eg- selected text, the start and end offsets, the total number of characters, and more.*/
    override fun getExtractedText(request: ExtractedTextRequest, flags: Int): ExtractedText? {
        if (mTextView != null) {
            val et = ExtractedText()
            if (mTextView.extractText(request, et)) {
                if (flags and GET_EXTRACTED_TEXT_MONITOR != 0) {
//                    mTextView.setExtracting(request);
                }
                return et
            }
        }
        return null
    }

//    API to send private commands from an input method to its connected editor. This can be used to provide domain-specific features
    override fun performPrivateCommand(action: String, data: Bundle): Boolean {
        mTextView!!.onPrivateIMECommand(action, data)
        return true
    }

    //send the text to the connected editor from the keyboard pressed
    override fun commitText(
        text: CharSequence,
        newCursorPosition: Int
    ): Boolean {
        if (mTextView == null) {
            return super.commitText(text, newCursorPosition)
        }
        if (text is Spanned) {
            text.getSpans(
                0, text.length,
                SuggestionSpan::class.java
            )
            //            mIMM.registerSuggestionSpansForNotification(spans);
        }

//        mTextView.resetErrorChangedFlag();
        //        mTextView.hideErrorIfUnchanged();
        return super.commitText(text, newCursorPosition)
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        if (DEBUG) Log.v(
            TAG,
            "requestUpdateCursorAnchorInfo $cursorUpdateMode"
        )

        // It is possible that any other bit is used as a valid flag in a future release.
        // We should reject the entire request in such a case.
        val KNOWN_FLAGS_MASK = CURSOR_UPDATE_IMMEDIATE or CURSOR_UPDATE_MONITOR
        val unknownFlags = cursorUpdateMode and KNOWN_FLAGS_MASK.inv()
        if (unknownFlags != 0) {
            if (DEBUG) {
                Log.d(
                    TAG,
                    "Rejecting requestUpdateCursorAnchorInfo due to unknown flags. cursorUpdateMode=$cursorUpdateMode unknownFlags=$unknownFlags"
                )
            }
            return false
        }
        return false
    }

    companion object {
        private const val DEBUG = false
        private val TAG = "loool"
    }
}
