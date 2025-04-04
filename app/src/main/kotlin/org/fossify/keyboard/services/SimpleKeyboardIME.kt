package org.fossify.keyboard.services

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.icu.text.BreakIterator
import android.icu.util.ULocale
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.text.InputType.*
import android.text.TextUtils
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.*
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
import android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
import android.view.inputmethod.EditorInfo.IME_MASK_ACTION
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import org.fossify.keyboard.R
import org.fossify.keyboard.databinding.KeyboardViewKeyboardBinding
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.getKeyboardBackgroundColor
import org.fossify.keyboard.extensions.getStrokeColor
import org.fossify.keyboard.extensions.safeStorageContext
import org.fossify.keyboard.helpers.*
import org.fossify.keyboard.interfaces.OnKeyboardActionListener
import org.fossify.keyboard.views.MyKeyboardView
import java.io.ByteArrayOutputStream
import java.util.Locale


// based on https://www.androidauthority.com/lets-build-custom-keyboard-android-832362/
class SimpleKeyboardIME : InputMethodService(), OnKeyboardActionListener, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        // How quickly do we have to double tap shift to enable permanent caps lock
        private var SHIFT_PERM_TOGGLE_SPEED = 500

        // Keyboard modes
        const val KEYBOARD_LETTERS = 0
        const val KEYBOARD_SYMBOLS = 1
        const val KEYBOARD_SYMBOLS_SHIFT = 2
        const val KEYBOARD_NUMBERS = 3
        const val KEYBOARD_PHONE = 4
        const val KEYBOARD_SYMBOLS_ALT = 5
    }

    private var keyboard: MyKeyboard? = null
    private var keyboardView: MyKeyboardView? = null
    private var lastShiftPressTS = 0L
    private var keyboardMode = KEYBOARD_LETTERS
    private var inputTypeClass = TYPE_CLASS_TEXT
    private var inputTypeClassVariation = TYPE_CLASS_TEXT
    private var enterKeyType = IME_ACTION_NONE
    private var switchToLetters = false
    private var breakIterator: BreakIterator? = null

    private lateinit var binding: KeyboardViewKeyboardBinding

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        safeStorageContext.getSharedPrefs().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateInputView(): View {
        binding = KeyboardViewKeyboardBinding.inflate(layoutInflater)
        keyboardView = binding.keyboardView.apply {
            setKeyboardHolder(binding)
            setKeyboard(keyboard!!)
            setEditorInfo(currentInputEditorInfo)
            setupNavigationBarPadding()
            mOnKeyboardActionListener = this@SimpleKeyboardIME
        }

        return binding.root
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        updateBackgroundColors()
    }

    override fun onPress(primaryCode: Int) {
        if (primaryCode != 0) {
            keyboardView?.vibrateIfNeeded()
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        inputTypeClass = attribute!!.inputType and TYPE_MASK_CLASS
        inputTypeClassVariation = attribute.inputType and TYPE_MASK_VARIATION
        enterKeyType = attribute.imeOptions and (IME_MASK_ACTION or IME_FLAG_NO_ENTER_ACTION)
        keyboard = createNewKeyboard()
        keyboardView?.setKeyboard(keyboard!!)
        keyboardView?.setEditorInfo(attribute)
        if (isNougatPlus()) {
            breakIterator = BreakIterator.getCharacterInstance(ULocale.getDefault())
        }
        updateShiftKeyState()
    }

    private fun updateShiftKeyState() {
        if (keyboard?.mShiftState == ShiftState.ON_PERMANENT) {
            return
        }

        val editorInfo = currentInputEditorInfo
        if (config.enableSentencesCapitalization && editorInfo != null && editorInfo.inputType != TYPE_NULL) {
            if (currentInputConnection.getCursorCapsMode(editorInfo.inputType) != 0) {
                keyboard?.setShifted(ShiftState.ON_ONE_CHAR)
                keyboardView?.invalidateAllKeys()
                return
            }
        }

        keyboard?.setShifted(ShiftState.OFF)
        keyboardView?.invalidateAllKeys()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest {
        val maxWidth = resources.getDimensionPixelSize(R.dimen.suggestion_max_width)

        return InlineSuggestionsRequest.Builder(
            listOf(
                InlinePresentationSpec.Builder(
                    Size(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                    Size(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                ).setStyle(buildSuggestionTextStyle()).build()
            )
        ).setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        keyboardView?.clearClipboardViews()

        response.inlineSuggestions.forEach {
            it.inflate(this, Size(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT), this.mainExecutor) { view ->
                // If inflation fails for whatever reason, passed view will be null
                if (view != null) {
                    keyboardView?.addToClipboardViews(view, addToFront = it.info.isPinned)
                }
            }
        }

        return true
    }

    override fun onKey(code: Int) {
        val inputConnection = currentInputConnection
        if (keyboard == null || inputConnection == null) {
            return
        }

        if (code != MyKeyboard.KEYCODE_SHIFT) {
            lastShiftPressTS = 0
        }

        when (code) {
            MyKeyboard.KEYCODE_DELETE -> {
                val selectedText = inputConnection.getSelectedText(0)
                if (TextUtils.isEmpty(selectedText)) {
                    val count = getCountToDelete(inputConnection)
                    inputConnection.deleteSurroundingText(count, 0)
                } else {
                    inputConnection.commitText("", 1)
                }
            }

            MyKeyboard.KEYCODE_SHIFT -> {
                if (keyboardMode == KEYBOARD_LETTERS) {
                    when {
                        keyboard!!.mShiftState == ShiftState.ON_PERMANENT -> keyboard!!.mShiftState = ShiftState.OFF
                        System.currentTimeMillis() - lastShiftPressTS < SHIFT_PERM_TOGGLE_SPEED -> keyboard!!.mShiftState = ShiftState.ON_PERMANENT
                        keyboard!!.mShiftState == ShiftState.ON_ONE_CHAR -> keyboard!!.mShiftState = ShiftState.OFF
                        keyboard!!.mShiftState == ShiftState.OFF -> keyboard!!.mShiftState = ShiftState.ON_ONE_CHAR
                    }

                    lastShiftPressTS = System.currentTimeMillis()
                } else {
                    val keyboardXml = if (keyboardMode == KEYBOARD_SYMBOLS) {
                        keyboardMode = KEYBOARD_SYMBOLS_SHIFT
                        R.xml.keys_symbols_shift
                    } else {
                        keyboardMode = KEYBOARD_SYMBOLS
                        R.xml.keys_symbols
                    }
                    keyboard = MyKeyboard(this, keyboardXml, enterKeyType)
                    keyboardView!!.setKeyboard(keyboard!!)
                }
                keyboardView!!.invalidateAllKeys()
            }

            MyKeyboard.KEYCODE_ENTER -> {
                val imeOptionsActionId = getImeOptionsActionId()
                if (imeOptionsActionId != IME_ACTION_NONE) {
                    inputConnection.performEditorAction(imeOptionsActionId)
                } else {
                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }

            MyKeyboard.KEYCODE_SYMBOLS_MODE_CHANGE -> {
                val keyboardXML = if (keyboardMode == KEYBOARD_SYMBOLS || keyboardMode == KEYBOARD_SYMBOLS_SHIFT) {
                    keyboardMode = KEYBOARD_SYMBOLS_ALT
                    R.xml.keys_symbols_alt
                } else {
                    keyboardMode = KEYBOARD_SYMBOLS
                    R.xml.keys_symbols
                }

                keyboard = MyKeyboard(this, keyboardXML, enterKeyType)
                keyboardView!!.setKeyboard(keyboard!!)
            }

            MyKeyboard.KEYCODE_MODE_CHANGE -> {
                val keyboardXml = if (keyboardMode == KEYBOARD_LETTERS) {
                    keyboardMode = KEYBOARD_SYMBOLS
                    R.xml.keys_symbols
                } else {
                    keyboardMode = KEYBOARD_LETTERS
                    getKeyboardLayoutXML()
                }

                keyboard = MyKeyboard(this, keyboardXml, enterKeyType)
                keyboardView!!.setKeyboard(keyboard!!)
            }

            MyKeyboard.KEYCODE_EMOJI -> {
                keyboardView?.openEmojiPalette()
            }

            else -> {
                var codeChar = code.toChar()
                val originalText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)?.text

                if (Character.isLetter(codeChar) && keyboard!!.mShiftState > ShiftState.OFF) {
                    if (baseContext.config.keyboardLanguage == LANGUAGE_TURKISH_Q) {
                        codeChar = codeChar.toString().uppercase(Locale.forLanguageTag("tr")).single()
                    } else {
                        codeChar = Character.toUpperCase(codeChar)
                    }
                }

                // If the keyboard is set to symbols and the user presses space, we usually should switch back to the letters keyboard.
                // However, avoid doing that in cases when the EditText for example requires numbers as the input.
                // We can detect that by the text not changing on pressing Space.
                if (keyboardMode != KEYBOARD_LETTERS && inputTypeClass == TYPE_CLASS_TEXT && code == MyKeyboard.KEYCODE_SPACE) {
                    inputConnection.commitText(codeChar.toString(), 1)
                    val newText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)?.text
                    if (originalText != newText) {
                        switchToLetters = keyboardMode != KEYBOARD_SYMBOLS_ALT
                    }
                } else {
                    when {
                        !originalText.isNullOrEmpty() && cachedVNTelexData.isNotEmpty() -> {
                            val fullText = originalText.toString() + codeChar.toString()
                            val lastIndexEmpty = if (fullText.contains(" ")) {
                                fullText.lastIndexOf(" ")
                            } else 0
                            if (lastIndexEmpty >= 0) {
                                val word = fullText.subSequence(lastIndexEmpty, fullText.length).trim().toString()
                                val wordChars = word.toCharArray()
                                val predictWord = StringBuilder()
                                for (char in wordChars.size - 1 downTo 0) {
                                    predictWord.append(wordChars[char])
                                    val shouldChangeText = predictWord.reverse().toString()
                                    if (cachedVNTelexData.containsKey(shouldChangeText)) {
                                        inputConnection.setComposingRegion(fullText.length - shouldChangeText.length, fullText.length)
                                        inputConnection.setComposingText(cachedVNTelexData[shouldChangeText], fullText.length)
                                        inputConnection.setComposingRegion(fullText.length, fullText.length)
                                        return
                                    }
                                }
                                inputConnection.commitText(codeChar.toString(), 1)
                                updateShiftKeyState()
                            }
                        }

                        else -> {
                            inputConnection.commitText(codeChar.toString(), 1)
                            updateShiftKeyState()
                        }
                    }
                }
            }
        }
    }

    private fun getCountToDelete(inputConnection: InputConnection): Int {
        if (breakIterator == null || !isNougatPlus()) {
            return 1
        }

        val prevText = inputConnection.getTextBeforeCursor(8, 0)


        if (!TextUtils.isEmpty(prevText)) {
            return breakIterator?.let {
                it.setText(prevText.toString())
                val end = it.last()
                val start = it.previous()
                (end - (if (start == BreakIterator.DONE) 0 else start)).coerceIn(0, prevText?.length)
            } ?: 1
        }

        return 1
    }

    override fun onActionUp() {
        if (switchToLetters) {
            // TODO: Change keyboardMode to enum class
            keyboardMode = KEYBOARD_LETTERS

            keyboard = MyKeyboard(this, getKeyboardLayoutXML(), enterKeyType)

            val editorInfo = currentInputEditorInfo
            if (editorInfo != null && editorInfo.inputType != TYPE_NULL && keyboard?.mShiftState != ShiftState.ON_PERMANENT) {
                if (currentInputConnection.getCursorCapsMode(editorInfo.inputType) != 0) {
                    keyboard?.setShifted(ShiftState.ON_ONE_CHAR)
                }
            }

            keyboardView!!.setKeyboard(keyboard!!)
            switchToLetters = false
        }
    }

    override fun moveCursorLeft() {
        moveCursor(false)
    }

    override fun moveCursorRight() {
        moveCursor(true)
    }

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun reloadKeyboard() {
        val keyboard = createNewKeyboard()
        this.keyboard = keyboard
        keyboardView?.setKeyboard(keyboard)
    }

    override fun changeInputMethod(id: String, subtype: InputMethodSubtype) {
        if (isPiePlus()) {
            switchInputMethod(id, subtype)
        } else {
            switchInputMethod(id)
        }
    }

    private fun createNewKeyboard(): MyKeyboard {
        val keyboardXml = when (inputTypeClass) {
            TYPE_CLASS_NUMBER -> {
                keyboardMode = KEYBOARD_NUMBERS
                R.xml.keys_numbers
            }

            TYPE_CLASS_PHONE -> {
                keyboardMode = KEYBOARD_PHONE
                R.xml.keys_phone
            }

            TYPE_CLASS_DATETIME -> {
                keyboardMode = KEYBOARD_SYMBOLS
                R.xml.keys_symbols
            }

            else -> {
                keyboardMode = KEYBOARD_LETTERS
                getKeyboardLayoutXML()
            }
        }
        return MyKeyboard(
            context = this,
            xmlLayoutResId = keyboardXml,
            enterKeyType = enterKeyType,
        )
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (newSelStart == newSelEnd) {
            keyboardView?.closeClipboardManager()
        }
        updateShiftKeyState()
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        updateShiftKeyState()
    }

    private fun moveCursor(moveRight: Boolean) {
        val extractedText = currentInputConnection?.getExtractedText(ExtractedTextRequest(), 0) ?: return
        var newCursorPosition = extractedText.selectionStart
        newCursorPosition = if (moveRight) {
            newCursorPosition + 1
        } else {
            newCursorPosition - 1
        }

        currentInputConnection?.setSelection(newCursorPosition, newCursorPosition)
    }

    private fun getImeOptionsActionId(): Int {
        return if (currentInputEditorInfo.imeOptions and IME_FLAG_NO_ENTER_ACTION != 0) {
            IME_ACTION_NONE
        } else {
            currentInputEditorInfo.imeOptions and IME_MASK_ACTION
        }
    }

    private fun getKeyboardLayoutXML(): Int {
        return when (baseContext.config.keyboardLanguage) {
            LANGUAGE_ARABIC -> R.xml.keys_letters_arabic
            LANGUAGE_BELARUSIAN_CYRL -> R.xml.keys_letters_belarusian_cyrl
            LANGUAGE_BELARUSIAN_LATN -> R.xml.keys_letters_belarusian_latn
            LANGUAGE_BENGALI -> R.xml.keys_letters_bengali
            LANGUAGE_BULGARIAN -> R.xml.keys_letters_bulgarian
            LANGUAGE_CENTRAL_KURDISH -> R.xml.keys_letters_central_kurdish
            LANGUAGE_CHUVASH -> R.xml.keys_letters_chuvash
            LANGUAGE_DANISH -> R.xml.keys_letters_danish
            LANGUAGE_ENGLISH_DVORAK -> R.xml.keys_letters_english_dvorak
            LANGUAGE_ENGLISH_QWERTZ -> R.xml.keys_letters_english_qwertz
            LANGUAGE_ESPERANTO -> R.xml.keys_letters_esperanto
            LANGUAGE_FRENCH_AZERTY -> R.xml.keys_letters_french_azerty
            LANGUAGE_FRENCH_BEPO -> R.xml.keys_letters_french_bepo
            LANGUAGE_GERMAN -> R.xml.keys_letters_german
            LANGUAGE_GREEK -> R.xml.keys_letters_greek
            LANGUAGE_HEBREW -> R.xml.keys_letters_hebrew
            LANGUAGE_KABYLE_AZERTY -> R.xml.keys_letters_kabyle_azerty
            LANGUAGE_LITHUANIAN -> R.xml.keys_letters_lithuanian
            LANGUAGE_NORWEGIAN -> R.xml.keys_letters_norwegian
            LANGUAGE_POLISH -> R.xml.keys_letters_polish
            LANGUAGE_ROMANIAN -> R.xml.keys_letters_romanian
            LANGUAGE_RUSSIAN -> R.xml.keys_letters_russian
            LANGUAGE_SLOVENIAN -> R.xml.keys_letters_slovenian
            LANGUAGE_SWEDISH -> R.xml.keys_letters_swedish
            LANGUAGE_SPANISH -> R.xml.keys_letters_spanish_qwerty
            LANGUAGE_TURKISH_Q -> R.xml.keys_letters_turkish_q
            LANGUAGE_UKRAINIAN -> R.xml.keys_letters_ukrainian
            else -> R.xml.keys_letters_english_qwerty
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi", "UseCompatLoadingForDrawables")
    private fun buildSuggestionTextStyle(): Bundle {
        val stylesBuilder = UiVersions.newStylesBuilder()

        val verticalPadding = resources.getDimensionPixelSize(R.dimen.small_margin)
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.activity_margin)

        val textSize = resources.getDimension(R.dimen.label_text_size) / resources.displayMetrics.scaledDensity

        val rippleBg = resources.getDrawable(R.drawable.clipboard_background, theme) as RippleDrawable
        val layerDrawable = rippleBg.findDrawableByLayerId(R.id.clipboard_background_holder) as LayerDrawable
        layerDrawable.findDrawableByLayerId(R.id.clipboard_background_stroke).applyColorFilter(getStrokeColor())
        layerDrawable.findDrawableByLayerId(R.id.clipboard_background_shape).applyColorFilter(getProperBackgroundColor())

        val maxWidth = resources.getDimensionPixelSize(R.dimen.suggestion_max_width)
        val height = resources.getDimensionPixelSize(R.dimen.label_text_size) + verticalPadding * 2
        val chipBackgroundIcon: Icon = rippleBg.toBitmap(width = maxWidth, height = height).toIcon()

        val chipStyle =
            ViewStyle.Builder()
                // don't use Icon.createWithBitmap(), it crashes the app. Issue https://github.com/SimpleMobileTools/Simple-Keyboard/issues/248
                .setBackground(chipBackgroundIcon)
                .setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                .build()

        val iconStyle = ImageViewStyle.Builder().build()

        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(chipStyle)
            .setChipStyle(chipStyle)
            .setStartIconStyle(iconStyle)
            .setEndIconStyle(iconStyle)
            .setSingleIconChipIconStyle(iconStyle)
            .setTitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(0, 0, horizontalPadding, 0)
                    .setTextColor(getProperTextColor())
                    .setTextSize(textSize)
                    .build()
            )
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setTextColor(getProperTextColor())
                    .setTextSize(textSize)
                    .build()
            )
            .build()
        stylesBuilder.addStyle(style)
        return stylesBuilder.build()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != null && key in arrayOf(
                SHOW_KEY_BORDERS, KEYBOARD_LANGUAGE, HEIGHT_PERCENTAGE, SHOW_NUMBERS_ROW, VOICE_INPUT_METHOD,
                TEXT_COLOR, BACKGROUND_COLOR, PRIMARY_COLOR, ACCENT_COLOR, CUSTOM_TEXT_COLOR, CUSTOM_BACKGROUND_COLOR,
                CUSTOM_PRIMARY_COLOR, CUSTOM_ACCENT_COLOR, IS_GLOBAL_THEME_ENABLED, IS_SYSTEM_THEME_ENABLED
            )
        ) {
            keyboardView?.setupKeyboard()
            updateBackgroundColors()
        }
    }

    private fun setupNavigationBarPadding() {
        window.window?.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            ViewCompat.setOnApplyWindowInsetsListener(binding.keyboardHolder) { view, insets ->
                val bottomPadding = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                binding.keyboardHolder.updatePadding(bottom = bottomPadding)
                ViewCompat.onApplyWindowInsets(view, insets)
            }
        }
    }

    private fun updateBackgroundColors() {
        val backgroundColor = safeStorageContext.getKeyboardBackgroundColor()
        binding.keyboardHolder.setBackgroundColor(backgroundColor)
        window.window?.updateNavigationBarColors(backgroundColor)
    }

    private fun Bitmap.toIcon(): Icon {
        val byteArray: ByteArray = ByteArrayOutputStream().let { outputStream ->
            this.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.toByteArray()
        }
        this.recycle()

        return Icon.createWithData(byteArray, 0, byteArray.size)
    }
}
