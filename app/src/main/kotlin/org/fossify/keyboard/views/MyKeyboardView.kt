package org.fossify.keyboard.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SearchView
import android.widget.TextView
import android.widget.inline.InlineContentView
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updateMarginsRelative
import androidx.emoji2.text.EmojiCompat
import com.rishabh.emojipicker.EmojiPickedFromSuggestion
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beInvisibleIf
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.darkenColor
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.lightenColor
import org.fossify.commons.extensions.performHapticFeedback
import org.fossify.commons.extensions.removeUnderlines
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isOreoMr1Plus
import org.fossify.commons.helpers.isPiePlus
import org.fossify.keyboard.R
import org.fossify.keyboard.activities.ManageClipboardItemsActivity
import org.fossify.keyboard.activities.SettingsActivity
import org.fossify.keyboard.adapters.ClipsKeyboardAdapter
import org.fossify.keyboard.databinding.KeyboardKeyPreviewBinding
import org.fossify.keyboard.databinding.KeyboardPopupKeyboardBinding
import org.fossify.keyboard.databinding.KeyboardViewKeyboardBinding
import org.fossify.keyboard.dialogs.SwitchLanguageDialog
import org.fossify.keyboard.extensions.clipsDB
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.getCurrentClip
import org.fossify.keyboard.extensions.getCurrentVoiceInputMethod
import org.fossify.keyboard.extensions.getKeyboardBackgroundColor
import org.fossify.keyboard.extensions.getStrokeColor
import org.fossify.keyboard.extensions.isDeviceLocked
import org.fossify.keyboard.extensions.safeStorageContext
import org.fossify.keyboard.helpers.AccessHelper
import org.fossify.keyboard.helpers.LANGUAGE_TURKISH_Q
import org.fossify.keyboard.helpers.LANGUAGE_VIETNAMESE_TELEX
import org.fossify.keyboard.helpers.LANGUAGE_VN_TELEX
import org.fossify.keyboard.helpers.MAX_KEYS_PER_MINI_ROW
import org.fossify.keyboard.helpers.MyKeyboard
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_DELETE
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_EMOJI
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_ENTER
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_MODE_CHANGE
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SHIFT
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SPACE
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SYMBOLS_MODE_CHANGE
import org.fossify.keyboard.helpers.ShiftState
import org.fossify.keyboard.helpers.cachedVNTelexData
import org.fossify.keyboard.helpers.parseRawJsonSpecsFile
import org.fossify.keyboard.interfaces.OnKeyboardActionListener
import org.fossify.keyboard.interfaces.RefreshClipsListener
import org.fossify.keyboard.models.Clip
import org.fossify.keyboard.models.ClipsSectionLabel
import org.fossify.keyboard.models.ListItem
import java.util.Arrays
import java.util.Locale
import kotlin.toString

@SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
class MyKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleRes) {

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return if (accessHelper?.dispatchHoverEvent(event) == true) {
            true
        } else {
            super.dispatchHoverEvent(event)
        }
    }



    private var keyboardPopupBinding: KeyboardPopupKeyboardBinding? = null
    private var keyboardViewBinding: KeyboardViewKeyboardBinding? = null

    private var accessHelper: AccessHelper? = null

    private var mKeyboard: MyKeyboard? = null
    private var mCurrentKeyIndex: Int = NOT_A_KEY

    private var mLabelTextSize = 0
    private var mKeyTextSize = 0

    private var mTextColor = 0
    private var mBackgroundColor = 0
    private var mKeyboardBackgroundColor = 0
    private var mPrimaryColor = 0
    private var mStrokeColor = 0
    private var mKeyColor = 0
    private var mKeyColorPressed = 0

    private var mPreviewText: TextView? = null
    private val mPreviewPopup: PopupWindow
    private var mPreviewTextSizeLarge = 0
    private var mPreviewHeight = 0

    private val mCoordinates = IntArray(2)
    private val mPopupKeyboard: PopupWindow
    private var mMiniKeyboardContainer: View? = null
    private var mMiniKeyboard: MyKeyboardView? = null
    private var mMiniKeyboardOnScreen = false
    private var mPopupParent: View
    private var mMiniKeyboardOffsetX = 0
    private var mMiniKeyboardOffsetY = 0
    private val mMiniKeyboardCache: MutableMap<MyKeyboard.Key, View?>
    private var mKeys = ArrayList<MyKeyboard.Key>()
    private var mMiniKeyboardSelectedKeyIndex = -1

    var mOnKeyboardActionListener: OnKeyboardActionListener? = null
    private var mVerticalCorrection = 0
    private var mProximityThreshold = 0
    private var mPopupPreviewX = 0
    private var mPopupPreviewY = 0
    private var mLastX = 0
    private var mLastY = 0

    private val mPaint: Paint
    private var mDownTime = 0L
    private var mLastMoveTime = 0L
    private var mLastKey = 0
    private var mLastCodeX = 0
    private var mLastCodeY = 0
    private var mLastKeyPressedCode = 0
    private var mCurrentKey: Int = NOT_A_KEY
    private var mLastKeyTime = 0L
    private var mCurrentKeyTime = 0L
    private val mKeyIndices = IntArray(12)
    private var mPopupX = 0
    private var mPopupY = 0
    private var mRepeatKeyIndex = NOT_A_KEY
    private var mPopupLayout = 0
    private var mAbortKey = false
    private var mIsLongPressingSpace = false
    private var mLastSpaceMoveX = 0
    private var mPopupMaxMoveDistance = 0f
    private var mTopSmallNumberSize = 0f
    private var mTopSmallNumberMarginWidth = 0f
    private var mTopSmallNumberMarginHeight = 0f
    private val mSpaceMoveThreshold: Int
    private var ignoreTouches = false

    private var mKeyBackground: Drawable? = null
    private var mShowKeyBorders: Boolean = false
    private var mUsingSystemTheme: Boolean = true
    private var mVoiceInputMethod: String = ""

    private var mToolbarHolder: View? = null
    private var mClipboardManagerHolder: View? = null
    private var mEmojiPaletteHolder: View? = null
    private var emojiCompatMetadataVersion = 0

    // For multi-tap
    private var mLastTapTime = 0L

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted.  */
    private var mDrawPending = false

    /** The dirty region in the keyboard bitmap  */
    private val mDirtyRect = Rect()

    /** The keyboard bitmap for faster updates  */
    private var mBuffer: Bitmap? = null

    /** Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.  */
    private var mKeyboardChanged = false

    /** The canvas for the above mutable keyboard bitmap  */
    private var mCanvas: Canvas? = null

    private var mHandler: Handler? = null

    private var stopTextWatcherTopPopupSearchView = true


    companion object {
        private const val NOT_A_KEY = -1
        private val LONG_PRESSABLE_STATE_SET = intArrayOf(R.attr.state_long_pressable)
        private const val MSG_REMOVE_PREVIEW = 1
        private const val MSG_REPEAT = 2
        private const val MSG_LONGPRESS = 3
        private const val DELAY_AFTER_PREVIEW = 100
        private const val DEBOUNCE_TIME = 70
        private const val REPEAT_INTERVAL = 50 // ~20 keys per second
        private const val REPEAT_START_DELAY = 400
        private val LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()


        var typeTextInSearchView:String = ""

        /*it assign the imputmethod to the default one and remove from the keybaord*/
        var searching:Boolean = false
    }

    init {
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.MyKeyboardView, 0, defStyleRes)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val keyTextSize = 0
        val indexCnt = attributes.indexCount

        try {
            for (i in 0 until indexCnt) {
                when (val attr = attributes.getIndex(i)) {
                    R.styleable.MyKeyboardView_keyTextSize -> mKeyTextSize =
                        attributes.getDimensionPixelSize(attr, 18)
                }
            }
        } finally {
            attributes.recycle()
        }

        mPopupLayout = R.layout.keyboard_popup_keyboard
        mKeyBackground = resources.getDrawable(R.drawable.keyboard_key_selector, context.theme)
        mVerticalCorrection = resources.getDimension(R.dimen.vertical_correction).toInt()
        mLabelTextSize = resources.getDimension(R.dimen.label_text_size).toInt()
        mPreviewHeight = resources.getDimension(R.dimen.key_height).toInt()
        mSpaceMoveThreshold = resources.getDimension(R.dimen.medium_margin).toInt()

        with(safeStorageContext) {
            mTextColor = getProperTextColor()
            mBackgroundColor = getProperBackgroundColor()
            mKeyboardBackgroundColor = getKeyboardBackgroundColor()
            mPrimaryColor = getProperPrimaryColor()
            mStrokeColor = getStrokeColor()
        }

        mPreviewPopup = PopupWindow(context)
        mPreviewText = KeyboardKeyPreviewBinding.inflate(inflater).root
        mPreviewTextSizeLarge = context.resources.getDimension(R.dimen.preview_text_size).toInt()
        mPreviewPopup.contentView = mPreviewText
        mPreviewPopup.setBackgroundDrawable(null)

        mPreviewPopup.isTouchable = false
        mPopupKeyboard = PopupWindow(context)
        mPopupKeyboard.setBackgroundDrawable(null)
        mPopupParent = this
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.textSize = keyTextSize.toFloat()
        mPaint.textAlign = Align.CENTER
        mPaint.alpha = 255
        mMiniKeyboardCache = HashMap()
        mPopupMaxMoveDistance = resources.getDimension(R.dimen.popup_max_move_distance)
        mTopSmallNumberSize = resources.getDimension(R.dimen.small_text_size)
        mTopSmallNumberMarginWidth = resources.getDimension(R.dimen.top_small_number_margin_width)
        mTopSmallNumberMarginHeight = resources.getDimension(R.dimen.top_small_number_margin_height)
    }

    @SuppressLint("HandlerLeak")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mHandler == null) {
            mHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        MSG_REMOVE_PREVIEW -> mPreviewText!!.visibility = INVISIBLE
                        MSG_REPEAT -> if (repeatKey(false)) {
                            val repeat = Message.obtain(this, MSG_REPEAT)
                            sendMessageDelayed(repeat, REPEAT_INTERVAL.toLong())
                        }

                        MSG_LONGPRESS -> openPopupIfRequired(msg.obj as MotionEvent)
                    }
                }
            }
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        closeClipboardManager()
        closeEmojiPalette()

        if (visibility == VISIBLE) {
            setupKeyboard(changedView)
        }
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the view will re-layout itself to accommodate the keyboard.
     * @param keyboard the keyboard to display in this view
     */
    fun setKeyboard(keyboard: MyKeyboard) {
        if (mKeyboard != null) {
            showPreview(NOT_A_KEY)
        }

        closeClipboardManager()
        removeMessages()
        keyboardViewBinding?.topPopupSearchBarToolbar?.beGone()
        keyboardViewBinding?.emojiSearchView?.beVisible()
        keyboardViewBinding?.searchResultEmojiPickerView?.beGone()
        mKeyboard = keyboard
        val keys = mKeyboard!!.mKeys
        mKeys = keys!!.toMutableList() as ArrayList<MyKeyboard.Key>
        requestLayout()
        mKeyboardChanged = true
        invalidateAllKeys()
        computeProximityThreshold(keyboard)
        mMiniKeyboardCache.clear()
        mToolbarHolder?.beInvisibleIf(context.isDeviceLocked)

        accessHelper = AccessHelper(this, mKeyboard?.mKeys.orEmpty())
        ViewCompat.setAccessibilityDelegate(this, accessHelper)

        // Not really necessary to do every time, but will free up views
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        mAbortKey = true // Until the next ACTION_DOWN
    }

    /** Sets the top row above the keyboard containing a couple buttons and the clipboard **/
    fun setKeyboardHolder(binding: KeyboardViewKeyboardBinding) {
        keyboardViewBinding = binding.apply {
            mToolbarHolder = settingsCliboardToolbarHolder
            mClipboardManagerHolder = clipboardManagerHolder
            mEmojiPaletteHolder = emojiPaletteHolder

            voiceInputButton.setOnLongClickListener { context.toast(R.string.switch_to_voice_typing); true }
            voiceInputButton.setOnClickListener {
                val inputMethod = context.getCurrentVoiceInputMethod()
                if (inputMethod == null) {
                    context.toast(R.string.no_app_found)
                    return@setOnClickListener
                }

                val (im, type) = inputMethod
                mOnKeyboardActionListener?.changeInputMethod(im.id, type)
            }

            settingsCog.setOnLongClickListener { context.toast(R.string.settings); true; }
            settingsCog.setOnClickListener {
                vibrateIfNeeded()
                Intent(context, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            pinnedClipboardItems.setOnLongClickListener { context.toast(R.string.clipboard); true; }
            pinnedClipboardItems.setOnClickListener {
                vibrateIfNeeded()
                openClipboardManager()
            }

            clipboardClear.setOnLongClickListener { context.toast(R.string.clear_clipboard_data); true; }
            clipboardClear.setOnClickListener {
                vibrateIfNeeded()
                clearClipboardContent()
                toggleClipboardVisibility(false)
            }

            suggestionsHolder.addOnLayoutChangeListener(object : OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View?,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
                ) {
                    updateSuggestionsToolbarLayout()
                    binding.suggestionsHolder.removeOnLayoutChangeListener(this)
                }
            })

        }




        val clipboardManager =
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        clipboardManager.addPrimaryClipChangedListener {
            val clipboardContent = clipboardManager.primaryClip?.getItemAt(0)?.text?.trim()
            if (clipboardContent?.isNotEmpty() == true) {
                handleClipboard()
            }
            setupStoredClips()
        }

        binding.apply {
            clipboardManagerClose.setOnClickListener {
                vibrateIfNeeded()
                closeClipboardManager()
            }

            clipboardManagerManage.setOnLongClickListener { context.toast(R.string.manage_clipboard_items); true; }
            clipboardManagerManage.setOnClickListener {
                Intent(context, ManageClipboardItemsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            emojiPaletteClose.setOnClickListener {
                vibrateIfNeeded()
                closeEmojiPalette()
            }
        }

    }




    @SuppressLint("NotifyDataSetChanged")
    fun onEmojiSearchViewOperation() {
        keyboardViewBinding?.emojiSearchView?.setOnQueryTextListener(null)

        keyboardViewBinding?.emojiSearchView?.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                keyboardViewBinding?.apply {

                    topPopupSearchBarToolbar.beVisible()
                    stopTextWatcherTopPopupSearchView = true
                    topPopupSearchBar.text = SpannableStringBuilder(emojiSearchView.query.toString())
                    stopTextWatcherTopPopupSearchView = false
                    settingsCliboardToolbarHolder.beGone()
                    emojiSearchView.beGone()

                    searching = true
                    mOnKeyboardActionListener?.updateShiftStateToLowercase()
                    emojiSearchView.clearFocus()
                    topPopupSearchBar.requestFocus()
                    //to connect with otherInputConnection
                    mOnKeyboardActionListener?.searchViewFocused(topPopupSearchBar)






                    searchResultEmojiPickerView.beVisible()
                    emojiPaletteHolder.beGone()

                    /*It's s interface it runn when someone picked ffrom the emoji search result suggestion*/
                    val mainRecentProvider = keyboardViewBinding!!.emojiPickerView.getRecentEmojiProvider()
                    val emojipickedFromSuggestion: EmojiPickedFromSuggestion =
                        object : EmojiPickedFromSuggestion {
                            override fun pickedEmoji(emoji: String) {
                                searching = false
                                mOnKeyboardActionListener?.onText(emoji)
                                mainRecentProvider!!.recordSelection(emoji)
                                searching = true


                            }
                        }

                    //                        searchResultEmojiPickerView.build(mTextColor) {}//all the emoji is loaded

                    keyboardViewBinding!!.searchResultEmojiPickerView.setRecentEmojiProvider(mainRecentProvider!!)
                    keyboardViewBinding!!.emojiPickerView.setRecentEmojiProvider(mainRecentProvider)
                    searchResultEmojiPickerView.build(mTextColor) {
                        searchResultEmojiPickerView.usedInSearchResult = true


                        searchResultEmojiPickerView.emojiPickedFromSuggestion = (emojipickedFromSuggestion)


                        searchResultEmojiPickerView.emojiPickerItems = searchResultEmojiPickerView.buildEmojiPickerItems(true)
                        searchResultEmojiPickerView.bodyAdapter.hideTitleAndEmptyHint = true
                        searchResultEmojiPickerView.bodyAdapter.notifyDataSetChanged()


                    }





                    topPopupSearchBar.hint = resources.getString(R.string.search_emoji)


                }

            }
        }

        keyboardViewBinding?.apply {
            //clear the variable in the SimpleKeyhoardIME that holds keyboards typing
            popupToolbarCross.setOnClickListener(OnClickListener {
                stopTextWatcherTopPopupSearchView = true
                topPopupSearchBar.setText("")
                stopTextWatcherTopPopupSearchView = false
                typeTextInSearchView = ""
            })

            //on backpressed
            topPopupSearchBarBack.setOnClickListener {

                settingsCliboardToolbarHolder.beVisible()
                searching = false
                it.clearFocus()
                topPopupSearchBarToolbar.beGone()
                emojiSearchView.beVisible()
                searchResultEmojiPickerView.beGone()
                emojiPaletteHolder.beVisible()

            }


            //on realtime show emoji when typed
            var beforeTextEndPosition = 0


            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { beforeTextEndPosition = start }

                override fun onTextChanged(editable: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(editable: Editable?) {
                    if(!stopTextWatcherTopPopupSearchView) {
                        searchResultEmojiPickerView.emojiPickerItems = searchResultEmojiPickerView!!.buildEmojiPickerItems(false, editable.toString())
                        searchResultEmojiPickerView!!.bodyAdapter.notifyDataSetChanged()
                    }
                }
            }

            // Attach the TextWatcher initially

            topPopupSearchBar.addTextChangedListener(textWatcher)
        }
    }

    fun setEditorInfo(editorInfo: EditorInfo) {
        emojiCompatMetadataVersion =
            editorInfo.extras?.getInt(EmojiCompat.EDITOR_INFO_METAVERSION_KEY, 0) ?: 0
    }

    fun setupKeyboard(changedView: View? = null) {
        with(safeStorageContext) {
            mTextColor = getProperTextColor()
            mBackgroundColor = getProperBackgroundColor()
            mKeyboardBackgroundColor = getKeyboardBackgroundColor()
            mPrimaryColor = getProperPrimaryColor()
            mStrokeColor = getStrokeColor()

            mShowKeyBorders = config.showKeyBorders
            mUsingSystemTheme = isDynamicTheme()
            mVoiceInputMethod = config.voiceInputMethod
        }

        val isMainKeyboard = changedView == null || changedView.id != R.id.mini_keyboard_view
        mKeyColor = getKeyColor()
        mKeyColorPressed = mKeyColor.adjustAlpha(0.2f)
        mKeyBackground = if (mShowKeyBorders && isMainKeyboard) {
            resources.getDrawable(R.drawable.keyboard_key_selector_outlined, context.theme)
        } else {
            resources.getDrawable(R.drawable.keyboard_key_selector, context.theme)
        }

        if (!isMainKeyboard) {
            val previewBackground = background as LayerDrawable
            previewBackground.findDrawableByLayerId(R.id.button_background_shape)
                .applyColorFilter(mBackgroundColor)
            previewBackground.findDrawableByLayerId(R.id.button_background_stroke)
                .applyColorFilter(mStrokeColor)
            background = previewBackground
        } else {
            background.applyColorFilter(mKeyboardBackgroundColor)
        }

        val wasDarkened = mBackgroundColor != mBackgroundColor.darkenColor()
        keyboardViewBinding?.apply {
            topKeyboardDivider.beGoneIf(wasDarkened)
            topKeyboardDivider.background = ColorDrawable(mStrokeColor)
            mToolbarHolder?.background = ColorDrawable(mKeyboardBackgroundColor)

            clipboardValue.apply {
                background =
                    resources.getDrawable(R.drawable.clipboard_background, context.theme).apply {
                        val layerDrawable = (this as RippleDrawable)
                            .findDrawableByLayerId(R.id.clipboard_background_holder) as LayerDrawable
                        layerDrawable.findDrawableByLayerId(R.id.clipboard_background_stroke)
                            .applyColorFilter(mStrokeColor)
                        layerDrawable.findDrawableByLayerId(R.id.clipboard_background_shape)
                            .applyColorFilter(mBackgroundColor)
                    }

                setTextColor(mTextColor)
                setLinkTextColor(mTextColor)
            }

            settingsCog.applyColorFilter(mTextColor)
            pinnedClipboardItems.applyColorFilter(mTextColor)
            clipboardClear.applyColorFilter(mTextColor)
            voiceInputButton.applyColorFilter(mTextColor)
            voiceInputButton.beGoneIf(mVoiceInputMethod.isEmpty())

            mToolbarHolder?.beInvisibleIf(context.isDeviceLocked)

            topClipboardDivider.beGoneIf(wasDarkened)
            topClipboardDivider.background = ColorDrawable(mStrokeColor)
            clipboardManagerTopBar.background = ColorDrawable(mKeyboardBackgroundColor)
            clipboardManagerHolder.background = ColorDrawable(mBackgroundColor)

            clipboardManagerClose.applyColorFilter(mTextColor)
            clipboardManagerManage.applyColorFilter(mTextColor)

            clipboardManagerLabel.setTextColor(mTextColor)
            clipboardContentPlaceholder1.setTextColor(mTextColor)
            clipboardContentPlaceholder2.setTextColor(mTextColor)
        }

        setupEmojiPalette(
            toolbarColor = mKeyboardBackgroundColor,
            backgroundColor = mBackgroundColor,
            textColor = mTextColor
        )
        if (context.config.keyboardLanguage == LANGUAGE_VIETNAMESE_TELEX) {
            setupLanguageTelex()
        } else {
            cachedVNTelexData.clear()
        }
        setupStoredClips()
    }

    fun vibrateIfNeeded() {
        if (context.config.vibrateOnKeypress) {
            performHapticFeedback()
        }
    }

    fun performHapticHandleMove() {
        if (!context.config.vibrateOnKeypress) return
        if (isOreoMr1Plus()) {
            @Suppress("DEPRECATION")
            performHapticFeedback(
                HapticFeedbackConstants.TEXT_HANDLE_MOVE,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     */
    private fun setShifted(shiftState: ShiftState) {
        if (mKeyboard?.setShifted(shiftState) == true) {
            invalidateAllKeys()
        }
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     * @return true if the shift is in a pressed state, false otherwise
     */
    private fun isShifted(): Boolean {
        return (mKeyboard?.mShiftState ?: ShiftState.OFF) > ShiftState.OFF
    }

    private fun setPopupOffset(x: Int, y: Int) {
        mMiniKeyboardOffsetX = x
        mMiniKeyboardOffsetY = y
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
    }

    private fun adjustCase(label: CharSequence): CharSequence? {
        var newLabel: CharSequence? = label
        if (
            !newLabel.isNullOrEmpty()
            && mKeyboard!!.mShiftState != ShiftState.OFF
            && newLabel.length < 3
            && Character.isLowerCase(newLabel[0])
        ) {
            if (context.config.keyboardLanguage == LANGUAGE_TURKISH_Q) {
                newLabel = newLabel.toString().uppercase(Locale.forLanguageTag("tr"))
            } else {
                newLabel = newLabel.toString().uppercase(Locale.getDefault())
            }
        }
        return newLabel
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mKeyboard == null) {
            setMeasuredDimension(0, 0)
        } else {
            var width = mKeyboard!!.mMinWidth
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec)
            }
            setMeasuredDimension(width, mKeyboard!!.mHeight)
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically) and square it to get the proximity threshold. We use a square here and
     * in computing the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private fun computeProximityThreshold(keyboard: MyKeyboard?) {
        if (keyboard == null) {
            return
        }

        val keys = mKeys
        val length = keys.size
        var dimensionSum = 0
        for (i in 0 until length) {
            val key = keys[i]
            dimensionSum += Math.min(key.width, key.height) + key.gap
        }

        if (dimensionSum < 0 || length == 0) {
            return
        }

        mProximityThreshold = (dimensionSum * 1.4f / length).toInt()
        mProximityThreshold *= mProximityThreshold // Square it
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun onBufferDraw() {
        if (mBuffer == null || mKeyboardChanged) {
            if (mBuffer == null || mKeyboardChanged && (mBuffer!!.width != width || mBuffer!!.height != height)) {
                // Make sure our bitmap is at least 1x1
                val width = Math.max(1, width)
                val height = Math.max(1, height)
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                mCanvas = Canvas(mBuffer!!)
            }
            invalidateAllKeys()
            mKeyboardChanged = false
        }

        if (mKeyboard == null) {
            return
        }

        mCanvas!!.save()
        val canvas = mCanvas
        canvas!!.clipRect(mDirtyRect)
        val paint = mPaint
        val keys = mKeys
        paint.color = mTextColor
        val smallLetterPaint = Paint().apply {
            set(paint)
            color = paint.color.adjustAlpha(0.8f)
            textSize = mTopSmallNumberSize
            typeface = Typeface.DEFAULT
        }

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        handleClipboard()

        val keyCount = keys.size
        for (i in 0 until keyCount) {
            val key = keys[i]
            val code = key.code

            setupKeyBackground(key, code, canvas)
            val textColor = if (key.pressed) {
                mTextColor.adjustAlpha(0.5f)
            } else {
                mTextColor
            }

            // Switch the character to uppercase if shift is pressed
            val label = adjustCase(key.label)?.toString()
            if (label?.isNotEmpty() == true) {
                // For characters, use large font. For labels like "Done", use small font.
                if (label.length > 1) {
                    paint.textSize = mLabelTextSize.toFloat()
                    paint.typeface = Typeface.DEFAULT_BOLD
                } else {
                    paint.textSize = mKeyTextSize.toFloat()
                    paint.typeface = Typeface.DEFAULT
                }

                paint.color = textColor

                val rows = label.split("\n")
                val textSize = paint.textSize
                val startY =
                    (key.height / 2f) + ((textSize - paint.descent()) / 2f) - ((textSize / 2f) * (rows.size - 1))
                rows.forEachIndexed { index, row ->
                    canvas.drawText(row, key.width / 2f, startY + textSize * index, paint)
                }

                if (key.topSmallNumber.isNotEmpty() && !(context.config.showNumbersRow && Regex("\\d").matches(
                        key.topSmallNumber
                    ))
                ) {
                    val bounds = Rect().also {
                        smallLetterPaint.getTextBounds(
                            key.topSmallNumber,
                            0,
                            key.topSmallNumber.length,
                            it
                        )
                    }

                    smallLetterPaint.color = if (key.pressed) {
                        textColor
                    } else {
                        smallLetterPaint.color
                    }

                    canvas.drawText(
                        key.topSmallNumber,
                        key.width - bounds.width() / 2f - mTopSmallNumberMarginWidth,
                        key.y + mTopSmallNumberSize + mTopSmallNumberMarginHeight,
                        smallLetterPaint
                    )
                }

                // Turn off drop shadow
                paint.setShadowLayer(0f, 0f, 0f, 0)
            } else if (key.icon != null && mKeyboard != null) {
                if (code == KEYCODE_SHIFT) {
                    val drawableId = when (mKeyboard!!.mShiftState) {
                        ShiftState.OFF -> R.drawable.ic_caps_outline_vector
                        ShiftState.ON_ONE_CHAR -> R.drawable.ic_caps_vector
                        else -> R.drawable.ic_caps_underlined_vector
                    }
                    key.icon = resources.getDrawable(drawableId)
                }

                if (code == KEYCODE_ENTER) {
                    val contrastColor = mPrimaryColor.getContrastColor()
                    key.icon!!.applyColorFilter(contrastColor)
                    key.secondaryIcon?.applyColorFilter(contrastColor.adjustAlpha(0.6f))
                } else if (code == KEYCODE_DELETE || code == KEYCODE_SHIFT || code == KEYCODE_EMOJI) {
                    key.icon!!.applyColorFilter(textColor)
                    key.secondaryIcon?.applyColorFilter(
                        if (key.pressed) {
                            textColor
                        } else {
                            mTextColor.adjustAlpha(0.6f)
                        }
                    )
                }

                val keyIcon = key.icon!!
                val secondaryIcon = key.secondaryIcon
                if (secondaryIcon != null) {
                    val keyIconWidth = (keyIcon.intrinsicWidth * 0.9f).toInt()
                    val keyIconHeight = (keyIcon.intrinsicHeight * 0.9f).toInt()
                    val secondaryIconWidth = (secondaryIcon.intrinsicWidth * 0.5f).toInt()
                    val secondaryIconHeight = (secondaryIcon.intrinsicHeight * 0.5f).toInt()

                    val centerX = key.width / 2
                    val centerY = key.height / 2

                    val keyIconLeft = centerX - keyIconWidth / 2
                    val keyIconTop = centerY - keyIconHeight / 2

                    keyIcon.setBounds(
                        keyIconLeft,
                        keyIconTop,
                        keyIconLeft + keyIconWidth,
                        keyIconTop + keyIconHeight
                    )
                    keyIcon.draw(canvas)

                    val secondaryIconPaddingRight = 10
                    val secondaryIconLeft =
                        key.width - secondaryIconPaddingRight - secondaryIconWidth
                    val secondaryIconRight = secondaryIconLeft + secondaryIconWidth

                    val secondaryIconTop = 14 // This will act as a topPadding
                    val secondaryIconBottom = secondaryIconTop + secondaryIconHeight

                    secondaryIcon.setBounds(
                        secondaryIconLeft, secondaryIconTop, secondaryIconRight, secondaryIconBottom
                    )
                    secondaryIcon.draw(canvas)

                    secondaryIcon.draw(canvas)
                } else {
                    val drawableX = (key.width - keyIcon.intrinsicWidth) / 2
                    val drawableY = (key.height - keyIcon.intrinsicHeight) / 2
                    canvas.translate(drawableX.toFloat(), drawableY.toFloat())
                    keyIcon.setBounds(0, 0, keyIcon.intrinsicWidth, keyIcon.intrinsicHeight)
                    keyIcon.draw(canvas)
                    canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())
                }
            }
            canvas.translate(-key.x.toFloat(), -key.y.toFloat())
        }

        // Overlay a dark rectangle to dim the keyboard
        if (mMiniKeyboardOnScreen) {
            paint.color = mKeyboardBackgroundColor.adjustAlpha(0.6f)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        mCanvas!!.restore()
        mDrawPending = false
        mDirtyRect.setEmpty()
    }

    private fun setupKeyBackground(key: MyKeyboard.Key, keyCode: Int, canvas: Canvas) {
        val keyBackground = when {
            keyCode == KEYCODE_SPACE && key.label.isBlank() -> getSpaceKeyBackground()
            keyCode == KEYCODE_ENTER -> getEnterKeyBackground()
            else -> mKeyBackground
        }

        val bounds = keyBackground!!.bounds
        if (key.width != bounds.right || key.height != bounds.bottom) {
            keyBackground.setBounds(0, 0, key.width, key.height)
        }

        keyBackground.state = when {
            key.pressed -> intArrayOf(android.R.attr.state_pressed)
            key.focused -> intArrayOf(android.R.attr.state_focused)
            else -> intArrayOf()
        }

        if (key.focused || keyCode == KEYCODE_ENTER) {
            val keyColor = if (key.pressed) {
                mPrimaryColor.adjustAlpha(0.8f)
            } else {
                mPrimaryColor
            }
            keyBackground.applyColorFilter(keyColor)
        } else if (mShowKeyBorders) {
            val keyColor = if (key.pressed) {
                mKeyColorPressed
            } else {
                mKeyColor
            }

            keyBackground.applyColorFilter(keyColor)
        }

        canvas.translate(key.x.toFloat(), key.y.toFloat())
        keyBackground.draw(canvas)
    }

    private fun getSpaceKeyBackground(): Drawable? {
        if (mShowKeyBorders) {
            return mKeyBackground
        }

        val drawableId = if (mUsingSystemTheme) {
            R.drawable.keyboard_space_background_material
        } else {
            R.drawable.keyboard_space_background
        }

        return resources.getDrawable(drawableId, context.theme)
    }

    private fun getEnterKeyBackground(): Drawable? {
        val drawableId = if (mShowKeyBorders) {
            R.drawable.keyboard_enter_background_outlined
        } else {
            R.drawable.keyboard_enter_background
        }
        return resources.getDrawable(drawableId, context.theme)
    }

    private fun handleClipboard() {
        if (mToolbarHolder != null && mPopupParent.id != R.id.mini_keyboard_view && context.config.showClipboardContent) {
            val clipboardContent = context.getCurrentClip()
            if (clipboardContent?.isNotEmpty() == true) {
                keyboardViewBinding?.apply {
                    clipboardValue.apply {
                        text = clipboardContent
                        removeUnderlines()
                        setOnClickListener {
                            mOnKeyboardActionListener!!.onText(clipboardContent.toString())
                            vibrateIfNeeded()
                        }
                    }

                    toggleClipboardVisibility(true)
                }
            } else {
                hideClipboardViews()
            }
        } else {
            hideClipboardViews()
        }
    }

    private fun hideClipboardViews() {
        keyboardViewBinding?.apply {
            clipboardValue.beGone()
            clipboardValue.alpha = 0f
            clipboardClear.beGone()
            clipboardClear.alpha = 0f
        }
    }

    private fun clearClipboardContent() {
        val clipboardManager =
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager) ?: return
        if (isPiePlus()) {
            clipboardManager.clearPrimaryClip()
        } else {
            val clip = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(clip)
        }
    }

    private fun toggleClipboardVisibility(show: Boolean) {
        if (
            (show && keyboardViewBinding?.clipboardValue!!.alpha == 0f) ||
            (!show && keyboardViewBinding?.clipboardValue!!.alpha == 1f)
        ) {
            val newAlpha = if (show) 1f else 0f
            val animations = ArrayList<ObjectAnimator>()
            val clipboardValueAnimation = ObjectAnimator.ofFloat(
                keyboardViewBinding!!.clipboardValue, "alpha", newAlpha
            )
            animations.add(clipboardValueAnimation)

            val clipboardClearAnimation = ObjectAnimator.ofFloat(
                keyboardViewBinding!!.clipboardClear, "alpha", newAlpha
            )
            animations.add(clipboardClearAnimation)

            val animSet = AnimatorSet()
            animSet.playTogether(*animations.toTypedArray())
            animSet.duration = 150
            animSet.interpolator = AccelerateInterpolator()
            animSet.doOnStart {
                if (show) {
                    keyboardViewBinding?.clipboardValue?.beVisible()
                    keyboardViewBinding?.clipboardClear?.beVisible()
                }
            }
            animSet.doOnEnd {
                if (!show) {
                    keyboardViewBinding?.clipboardValue?.beGone()
                    keyboardViewBinding?.clipboardClear?.beGone()
                }
            }
            animSet.start()
        }
    }

    private fun getPressedKeyIndex(x: Int, y: Int): Int {
        return mKeys.indexOfFirst {
            it.isInside(x, y)
        }
    }

    private fun detectAndSendKey(index: Int, x: Int, y: Int, eventTime: Long) {
        if (index != NOT_A_KEY && index in mKeys.indices) {
            val key = mKeys[index]
            getPressedKeyIndex(x, y)
            mOnKeyboardActionListener!!.onKey(key.code)
            mLastTapTime = eventTime
        }
    }

    private fun showPreview(keyIndex: Int) {
        val oldKeyIndex = mCurrentKeyIndex
        val previewPopup = mPreviewPopup
        mCurrentKeyIndex = keyIndex

        if (!context.config.showPopupOnKeypress) {
            return
        }

        // If key changed and preview is on ...
        if (oldKeyIndex != mCurrentKeyIndex) {
            if (previewPopup.isShowing) {
                if (keyIndex == NOT_A_KEY) {
                    mHandler!!.sendMessageDelayed(
                        mHandler!!.obtainMessage(MSG_REMOVE_PREVIEW), DELAY_AFTER_PREVIEW.toLong()
                    )
                }
            }

            if (keyIndex != NOT_A_KEY) {
                showKey(keyIndex)
            }
        }
    }

    private fun showKey(keyIndex: Int) {
        val previewPopup = mPreviewPopup
        val keys = mKeys
        if (keyIndex < 0 || keyIndex >= mKeys.size) {
            return
        }

        val key = keys[keyIndex]
        if (key.icon != null) {
            mPreviewText!!.setCompoundDrawables(null, null, null, key.icon)
        } else {
            if (key.label.length > 1) {
                mPreviewText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize.toFloat())
                mPreviewText!!.typeface = Typeface.DEFAULT_BOLD
            } else {
                mPreviewText!!.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    mPreviewTextSizeLarge.toFloat()
                )
                mPreviewText!!.typeface = Typeface.DEFAULT
            }

            mPreviewText!!.setCompoundDrawables(null, null, null, null)
            try {
                mPreviewText!!.text = adjustCase(key.label)
            } catch (ignored: Exception) {
            }
        }

        val previewBackground = mPreviewText!!.background as LayerDrawable
        previewBackground.findDrawableByLayerId(R.id.button_background_shape)
            .applyColorFilter(mBackgroundColor)
        previewBackground.findDrawableByLayerId(R.id.button_background_stroke)
            .applyColorFilter(mStrokeColor)
        mPreviewText!!.background = previewBackground

        mPreviewText!!.setTextColor(mTextColor)
        mPreviewText!!.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = Math.max(mPreviewText!!.measuredWidth, key.width)
        val popupHeight = mPreviewHeight
        val lp = mPreviewText!!.layoutParams
        lp?.width = popupWidth
        lp?.height = popupHeight

        mPopupPreviewX = key.x
        mPopupPreviewY = key.y - popupHeight

        mHandler!!.removeMessages(MSG_REMOVE_PREVIEW)
        getLocationInWindow(mCoordinates)
        mCoordinates[0] += mMiniKeyboardOffsetX // Offset may be zero
        mCoordinates[1] += mMiniKeyboardOffsetY // Offset may be zero

        // Set the preview background state
        mPreviewText!!.background.state = if (key.popupResId != 0) {
            LONG_PRESSABLE_STATE_SET
        } else {
            EMPTY_STATE_SET
        }

        mPopupPreviewX += mCoordinates[0]
        mPopupPreviewY += mCoordinates[1]

        // If the popup cannot be shown above the key, put it on the side
        getLocationOnScreen(mCoordinates)
        if (mPopupPreviewY + mCoordinates[1] < 0) {
            // If the key you're pressing is on the left side of the keyboard, show the popup on
            // the right, offset by enough to see at least one key to the left/right.
            if (key.x + key.width <= width / 2) {
                mPopupPreviewX += (key.width * 2.5).toInt()
            } else {
                mPopupPreviewX -= (key.width * 2.5).toInt()
            }
            mPopupPreviewY += popupHeight
        }

        previewPopup.dismiss()

        if (
            key.label.isNotEmpty()
            && key.code != KEYCODE_MODE_CHANGE
            && key.code != KEYCODE_SYMBOLS_MODE_CHANGE
            && key.code != KEYCODE_SHIFT
        ) {
            previewPopup.width = popupWidth
            previewPopup.height = popupHeight
            previewPopup.showAtLocation(
                mPopupParent,
                Gravity.NO_GRAVITY,
                mPopupPreviewX,
                mPopupPreviewY
            )
            mPreviewText!!.visibility = VISIBLE
        }
    }

    /**
     * Requests a redraw of the entire keyboard. Calling [.invalidate] is not sufficient because the keyboard renders the keys to an off-screen buffer and
     * an invalidate() only draws the cached buffer.
     */
    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
        invalidate()
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only one key is changing it's content. Any changes that
     * affect the position or size of the key may not be honored.
     * @param keyIndex the index of the key in the attached [MyKeyboard].
     */
    private fun invalidateKey(keyIndex: Int) {
        if (keyIndex < 0 || keyIndex >= mKeys.size) {
            return
        }

        val key = mKeys[keyIndex]
        mDirtyRect.union(
            key.x, key.y,
            key.x + key.width, key.y + key.height
        )
        onBufferDraw()
        invalidate(
            key.x, key.y,
            key.x + key.width, key.y + key.height
        )
    }

    private fun openPopupIfRequired(me: MotionEvent): Boolean {
        // Check if we have a popup layout specified first.
        if (mPopupLayout == 0) {
            return false
        }

        if (mCurrentKey < 0 || mCurrentKey >= mKeys.size) {
            return false
        }

        val popupKey = mKeys[mCurrentKey]
        val result = onLongPress(popupKey, me)
        if (result) {
            mAbortKey = true
            showPreview(NOT_A_KEY)
        }

        return result
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated with this key through the attributes
     * popupLayout and popupCharacters.
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the method on the base class if the subclass doesn't wish to
     * handle the call.
     */
    private fun onLongPress(popupKey: MyKeyboard.Key, me: MotionEvent): Boolean {
        if (popupKey.code == KEYCODE_EMOJI) {
            setCurrentKeyPressed(false)
            SwitchLanguageDialog(this) {
                mOnKeyboardActionListener?.reloadKeyboard()
            }
            return true
        } else {
            val popupKeyboardId = popupKey.popupResId
            if (popupKeyboardId != 0) {
                mMiniKeyboardContainer = mMiniKeyboardCache[popupKey]

                // For 'number' and 'phone' keyboards the count of popup keys might be bigger than count of keys in the main keyboard.
                // And therefore the width of the key might be smaller than width declared in MyKeyboard.Key.width for the main keyboard.
                val popupKeyWidth = popupKey.calcKeyWidth(
                    containerWidth = mMiniKeyboardContainer?.measuredWidth ?: width
                )

                if (mMiniKeyboardContainer == null) {
                    val inflater =
                        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    keyboardPopupBinding = KeyboardPopupKeyboardBinding.inflate(inflater).apply {
                        mMiniKeyboardContainer = root
                        mMiniKeyboard = miniKeyboardView
                    }

                    val keyboard = if (popupKey.popupCharacters != null) {
                        MyKeyboard(
                            context = context,
                            layoutTemplateResId = popupKeyboardId,
                            characters = popupKey.popupCharacters!!,
                            keyWidth = popupKeyWidth
                        )
                    } else {
                        MyKeyboard(context, popupKeyboardId, 0)
                    }
                    mMiniKeyboard!!.setKeyboard(keyboard)
                    mPopupParent = this
                    mMiniKeyboardContainer!!.measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
                    )
                    mMiniKeyboardCache[popupKey] = mMiniKeyboardContainer
                } else {
                    mMiniKeyboard =
                        mMiniKeyboardCache[popupKey]?.let(KeyboardPopupKeyboardBinding::bind)?.miniKeyboardView
                }

                getLocationInWindow(mCoordinates)
                mPopupX = popupKey.x
                mPopupY = popupKey.y

                val widthToUse =
                    mMiniKeyboardContainer!!.measuredWidth - (popupKey.popupCharacters!!.length / 2) * popupKeyWidth
                mPopupX = mPopupX + popupKeyWidth - widthToUse
                mPopupY -= mMiniKeyboardContainer!!.measuredHeight
                val x = mPopupX + mCoordinates[0]
                val y = mPopupY + mCoordinates[1]
                val xOffset = Math.max(0, x)
                mMiniKeyboard!!.setPopupOffset(xOffset, y)

                // make sure we highlight the proper key right after long pressing it, before any ACTION_MOVE event occurs
                val miniKeyboardX = if (xOffset + mMiniKeyboard!!.measuredWidth <= measuredWidth) {
                    xOffset
                } else {
                    measuredWidth - mMiniKeyboard!!.measuredWidth
                }

                val keysCnt = mMiniKeyboard!!.mKeys.size
                var selectedKeyIndex =
                    Math.floor((me.x - miniKeyboardX) / popupKeyWidth.toDouble()).toInt()
                if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                    selectedKeyIndex += MAX_KEYS_PER_MINI_ROW
                }
                selectedKeyIndex = Math.max(0, Math.min(selectedKeyIndex, keysCnt - 1))

                for (i in 0 until keysCnt) {
                    mMiniKeyboard!!.mKeys[i].focused = i == selectedKeyIndex
                }

                mMiniKeyboardSelectedKeyIndex = selectedKeyIndex
                mMiniKeyboard!!.invalidateAllKeys()

                val miniShiftStatus = if (isShifted()) ShiftState.ON_PERMANENT else ShiftState.OFF
                mMiniKeyboard!!.setShifted(miniShiftStatus)
                mPopupKeyboard.contentView = mMiniKeyboardContainer
                mPopupKeyboard.width = mMiniKeyboardContainer!!.measuredWidth
                mPopupKeyboard.height = mMiniKeyboardContainer!!.measuredHeight
                mPopupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
                mMiniKeyboardOnScreen = true
                invalidateAllKeys()
                return true
            }
        }
        return false
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        val action = me.action

        if (ignoreTouches) {
            if (action == MotionEvent.ACTION_UP) {
                ignoreTouches = false

                // fix a glitch with long pressing backspace, then clicking some letter
                if (mRepeatKeyIndex != NOT_A_KEY) {
                    val key = mKeys.getOrNull(mRepeatKeyIndex)
                    if (key?.code == KEYCODE_DELETE) {
                        mHandler?.removeMessages(MSG_REPEAT)
                        mRepeatKeyIndex = NOT_A_KEY
                    }
                }
            }
            return true
        }

        // handle moving between alternative popup characters by swiping
        if (mPopupKeyboard.isShowing) {
            when (action) {
                MotionEvent.ACTION_MOVE -> {
                    if (mMiniKeyboard != null) {
                        val coords = intArrayOf(0, 0)
                        mMiniKeyboard!!.getLocationOnScreen(coords)
                        val keysCnt = mMiniKeyboard!!.mKeys.size
                        val lastRowKeyCount = if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                            Math.max(keysCnt % MAX_KEYS_PER_MINI_ROW, 1)
                        } else {
                            keysCnt
                        }

                        val widthPerKey = if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                            mMiniKeyboard!!.width / MAX_KEYS_PER_MINI_ROW
                        } else {
                            mMiniKeyboard!!.width / lastRowKeyCount
                        }

                        var selectedKeyIndex =
                            Math.floor((me.x - coords[0]) / widthPerKey.toDouble()).toInt()
                        if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                            selectedKeyIndex = Math.max(0, selectedKeyIndex)
                            selectedKeyIndex += MAX_KEYS_PER_MINI_ROW
                        }

                        selectedKeyIndex = Math.max(0, Math.min(selectedKeyIndex, keysCnt - 1))
                        if (selectedKeyIndex != mMiniKeyboardSelectedKeyIndex) {
                            for (i in 0 until keysCnt) {
                                mMiniKeyboard!!.mKeys[i].focused = i == selectedKeyIndex
                            }
                            mMiniKeyboardSelectedKeyIndex = selectedKeyIndex
                            mMiniKeyboard!!.invalidateAllKeys()
                        }

                        if (coords[0] > 0 || coords[1] > 0) {
                            if (coords[0] - me.x > mPopupMaxMoveDistance ||                                         // left
                                me.x - (coords[0] + mMiniKeyboard!!.measuredWidth) > mPopupMaxMoveDistance          // right
                            ) {
                                dismissPopupKeyboard()
                            }
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mMiniKeyboard?.mKeys?.firstOrNull { it.focused }?.apply {
                        mOnKeyboardActionListener!!.onKey(code)
                    }
                    mMiniKeyboardSelectedKeyIndex = -1
                    dismissPopupKeyboard()
                }
            }
        }

        return onModifiedTouchEvent(me)
    }

    private fun onModifiedTouchEvent(me: MotionEvent): Boolean {
        var touchX = me.x.toInt()
        var touchY = me.y.toInt()
        if (touchY >= -mVerticalCorrection) {
            touchY += mVerticalCorrection
        }

        val action = me.actionMasked
        val eventTime = me.eventTime
        val keyIndex = getPressedKeyIndex(touchX, touchY)

        // Ignore all motion events until a DOWN.
        if (mAbortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        // Needs to be called after the gesture detector gets a turn, as it may have displayed the mini keyboard
        if (mMiniKeyboardOnScreen && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                // if the user presses a key while still holding down the previous, type in both chars and ignore the later gestures
                // can happen at fast typing, easier to reproduce by increasing LONGPRESS_TIMEOUT
                ignoreTouches = true
                mHandler!!.removeMessages(MSG_LONGPRESS)
                dismissPopupKeyboard()
                detectAndSendKey(keyIndex, me.x.toInt(), me.y.toInt(), eventTime)

                val newPointerX = me.getX(1).toInt()
                val newPointerY = me.getY(1).toInt()
                val secondKeyIndex = getPressedKeyIndex(newPointerX, newPointerY)
                showPreview(secondKeyIndex)

                detectAndSendKey(secondKeyIndex, newPointerX, newPointerY, eventTime)

                val secondKeyCode = mKeys.getOrNull(secondKeyIndex)?.code
                if (secondKeyCode != null) {
                    mOnKeyboardActionListener!!.onPress(secondKeyCode)
                }

                showPreview(NOT_A_KEY)
                setCurrentKeyPressed(false)
                return true
            }

            MotionEvent.ACTION_DOWN -> {
                mAbortKey = false
                mLastCodeX = touchX
                mLastCodeY = touchY
                mLastKeyTime = 0
                mCurrentKeyTime = 0
                mLastKey = NOT_A_KEY
                mCurrentKey = keyIndex
                mDownTime = me.eventTime
                mLastMoveTime = mDownTime

                val onPressKey = if (keyIndex != NOT_A_KEY) {
                    mKeys[keyIndex].code
                } else {
                    0
                }
                mOnKeyboardActionListener!!.onPress(onPressKey)
                mLastKeyPressedCode = onPressKey

                var wasHandled = false
                if (mCurrentKey >= 0 && mKeys[mCurrentKey].repeatable) {
                    mRepeatKeyIndex = mCurrentKey

                    val msg = mHandler!!.obtainMessage(MSG_REPEAT)
                    mHandler!!.sendMessageDelayed(msg, REPEAT_START_DELAY.toLong())
                    // if the user long presses Space, move the cursor after swipine left/right
                    if (mKeys[mCurrentKey].code == KEYCODE_SPACE) {
                        mLastSpaceMoveX = -1
                    } else {
                        repeatKey(true)
                    }

                    // Delivering the key could have caused an abort
                    if (mAbortKey) {
                        mRepeatKeyIndex = NOT_A_KEY
                        wasHandled = true
                    }
                }

                if (!wasHandled && mCurrentKey != NOT_A_KEY) {
                    val msg = mHandler!!.obtainMessage(MSG_LONGPRESS, me)
                    mHandler!!.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                }

                if (mPopupParent.id != R.id.mini_keyboard_view) {
                    showPreview(keyIndex)
                    setCurrentKeyPressed(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (keyIndex != NOT_A_KEY) {
                    if (mCurrentKey == NOT_A_KEY) {
                        mCurrentKey = keyIndex
                        mCurrentKeyTime = eventTime - mDownTime
                    } else {
                        if (keyIndex == mCurrentKey) {
                            mCurrentKeyTime += eventTime - mLastMoveTime
                            continueLongPress = true
                        } else if (mRepeatKeyIndex == NOT_A_KEY) {
                            setCurrentKeyPressed(false)
                            mLastKey = mCurrentKey
                            mLastCodeX = mLastX
                            mLastCodeY = mLastY
                            mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                            mCurrentKey = keyIndex
                            mCurrentKeyTime = 0
                            setCurrentKeyPressed(true)
                        }
                    }
                }

                if (mIsLongPressingSpace) {
                    if (mLastSpaceMoveX == -1) {
                        mLastSpaceMoveX = mLastX
                    }

                    val diff = mLastX - mLastSpaceMoveX
                    if (diff < -mSpaceMoveThreshold) {
                        for (i in diff / mSpaceMoveThreshold until 0) {
                            mOnKeyboardActionListener?.moveCursorLeft()
                        }
                        mLastSpaceMoveX = mLastX
                    } else if (diff > mSpaceMoveThreshold) {
                        for (i in 0 until diff / mSpaceMoveThreshold) {
                            mOnKeyboardActionListener?.moveCursorRight()
                        }
                        mLastSpaceMoveX = mLastX
                    }
                } else if (!continueLongPress) {
                    // Cancel old longpress
                    mHandler!!.removeMessages(MSG_LONGPRESS)
                    // Start new longpress if key has changed
                    if (keyIndex != NOT_A_KEY) {
                        val msg = mHandler!!.obtainMessage(MSG_LONGPRESS, me)
                        mHandler!!.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                    }

                    if (mPopupParent.id != R.id.mini_keyboard_view) {
                        showPreview(mCurrentKey)
                    }
                    mLastMoveTime = eventTime
                }
            }

            MotionEvent.ACTION_UP -> {
                setCurrentKeyPressed(false)

                mLastSpaceMoveX = 0
                removeMessages()
                if (keyIndex == mCurrentKey) {
                    mCurrentKeyTime += eventTime - mLastMoveTime
                } else {
                    mLastKey = mCurrentKey
                    mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                    mCurrentKey = keyIndex
                    mCurrentKeyTime = 0
                }

                if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME && mLastKey != NOT_A_KEY) {
                    mCurrentKey = mLastKey
                    touchX = mLastCodeX
                    touchY = mLastCodeY
                }
                showPreview(NOT_A_KEY)
                Arrays.fill(mKeyIndices, NOT_A_KEY)

                val currentKeyCode = mKeys.getOrNull(mCurrentKey)?.code

                // If we're not on a repeating key (which sends on a DOWN event)
                if (mRepeatKeyIndex == NOT_A_KEY && !mMiniKeyboardOnScreen && !mAbortKey) {
                    detectAndSendKey(mCurrentKey, touchX, touchY, eventTime)
                } else if (currentKeyCode == KEYCODE_SPACE && !mIsLongPressingSpace) {
                    detectAndSendKey(mCurrentKey, touchX, touchY, eventTime)
                }

                mRepeatKeyIndex = NOT_A_KEY
                mOnKeyboardActionListener!!.onActionUp()
                mIsLongPressingSpace = false
            }

            MotionEvent.ACTION_CANCEL -> {
                mIsLongPressingSpace = false
                mLastSpaceMoveX = 0
                removeMessages()
                dismissPopupKeyboard()
                mAbortKey = true
                showPreview(NOT_A_KEY)
                setCurrentKeyPressed(false)
            }
        }

        mLastX = touchX
        mLastY = touchY
        return true
    }

    private fun repeatKey(initialCall: Boolean): Boolean {
        val key = mKeys[mRepeatKeyIndex]
        if (!initialCall && key.code == KEYCODE_SPACE) {
            if (!mIsLongPressingSpace) {
                vibrateIfNeeded()
            }

            mIsLongPressingSpace = true
        } else {
            detectAndSendKey(mCurrentKey, key.x, key.y, mLastTapTime)
        }
        return true
    }

    private fun setCurrentKeyPressed(pressed: Boolean) {
        mKeys.getOrNull(mCurrentKey)?.pressed = pressed
        invalidateKey(mCurrentKey)
    }

    fun closeClipboardManager() {
        keyboardViewBinding?.apply {
            clipboardManagerHolder.beGone()
            suggestionsHolder.showAllInlineContentViews()
        }
    }

    private fun openClipboardManager() {
        keyboardViewBinding?.apply {
            clipboardManagerHolder.beVisible()
            suggestionsHolder.hideAllInlineContentViews()
        }
        setupStoredClips()
    }

    private fun setupStoredClips() {
        ensureBackgroundThread {
            val clips = ArrayList<ListItem>()
            val clipboardContent = context.getCurrentClip()

            val pinnedClips = context.clipsDB.getClips()
            val isCurrentClipPinnedToo = pinnedClips.any {
                clipboardContent?.isNotEmpty() == true && it.value.trim() == clipboardContent
            }

            if (!isCurrentClipPinnedToo && clipboardContent?.isNotEmpty() == true) {
                val section = ClipsSectionLabel(context.getString(R.string.clipboard_current), true)
                clips.add(section)

                val clip = Clip(-1, clipboardContent)
                clips.add(clip)
            }

            if (!isCurrentClipPinnedToo && clipboardContent?.isNotEmpty() == true) {
                val section = ClipsSectionLabel(context.getString(R.string.clipboard_pinned), false)
                clips.add(section)
            }

            clips.addAll(pinnedClips)
            Handler(Looper.getMainLooper()).post {
                setupClipsAdapter(clips)
            }
        }
    }

    private fun setupClipsAdapter(clips: ArrayList<ListItem>) {
        keyboardViewBinding?.apply {
            clipboardContentPlaceholder1.beVisibleIf(clips.isEmpty())
            clipboardContentPlaceholder2.beVisibleIf(clips.isEmpty())
            clipsList.beVisibleIf(clips.isNotEmpty())
        }

        val refreshClipsListener = object : RefreshClipsListener {
            override fun refreshClips() {
                setupStoredClips()
            }
        }

        val adapter = ClipsKeyboardAdapter(
            context = safeStorageContext,
            items = clips,
            refreshClipsListener = refreshClipsListener
        ) { clip ->
            mOnKeyboardActionListener!!.onText(clip.value)
            vibrateIfNeeded()
        }

        keyboardViewBinding?.clipsList?.adapter = adapter
    }

    private fun setupEmojiPalette(toolbarColor: Int, backgroundColor: Int, textColor: Int) {
        keyboardViewBinding?.apply {
            emojiPaletteTopBar.background = ColorDrawable(toolbarColor)
            emojiPaletteHolder.background = ColorDrawable(backgroundColor)
            emojiPaletteClose.applyColorFilter(textColor)
            emojiPaletteLabel.setTextColor(textColor)
                // Get the internal EditText (works on all AOSP devices)
            val searchEditTextId = emojiSearchView.context.resources
                .getIdentifier("search_src_text", "id", "android")

            val searchEditText = emojiSearchView.findViewById<EditText>(searchEditTextId)

            searchEditText?.setTextColor(textColor)
            searchEditText?.setHintTextColor(textColor)

            tintSearchViewUnderline(emojiSearchView, textColor)

            topPopupSearchBarToolbar.background = ColorDrawable(toolbarColor)
            topPopupSearchBarBack.applyColorFilter(mTextColor)
            topPopupSearchBar.setTextColor(mTextColor)
            popupSearchIcon.applyColorFilter(mTextColor)
            popupToolbarCross.applyColorFilter(mTextColor)
            topPopupSearchBar.setHintTextColor(mTextColor)





            emojiPaletteBottomBar.background = ColorDrawable(toolbarColor)
            emojiPaletteModeChange.apply {
                setTextColor(textColor)
                setOnClickListener {
                    vibrateIfNeeded()
                    closeEmojiPalette()
                }
            }

            emojiPaletteBackspace.apply {
                applyColorFilter(textColor)
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressed = true
                            mRepeatKeyIndex = mKeys.indexOfFirst { it.code == KEYCODE_DELETE }
                            mCurrentKey = mRepeatKeyIndex
                            vibrateIfNeeded()
                            mOnKeyboardActionListener!!.onKey(KEYCODE_DELETE)
                            // setup repeating backspace
                            val msg = mHandler!!.obtainMessage(MSG_REPEAT)
                            mHandler!!.sendMessageDelayed(msg, REPEAT_START_DELAY.toLong())
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            mHandler!!.removeMessages(MSG_REPEAT)
                            mRepeatKeyIndex = NOT_A_KEY
                            isPressed = false
                            false
                        }

                        else -> false
                    }
                }
            }
        }

        keyboardViewBinding!!.emojiPickerView.build(mTextColor){}
    }

    fun tintSearchViewUnderline(searchView: SearchView, @ColorInt color: Int) {
        searchView.post {
            val res = searchView.context.resources


            val id = res.getIdentifier("search_plate", "id", "android")
            if (id != 0) {
                val view = searchView.findViewById<View>(id)
                view?.background?.let { bg ->
                    val wrapped = DrawableCompat.wrap(bg.mutate())
                    DrawableCompat.setTint(wrapped, color)
                    view.background = wrapped
                }
            }

        }
    }

    fun openEmojiPalette() {
        keyboardViewBinding!!.emojiPaletteHolder.beVisible()
        keyboardViewBinding!!.suggestionsHolder.beGone()
        keyboardViewBinding!!.suggestionsHolder.beGone()

        keyboardViewBinding!!.emojiSearchView.onActionViewExpanded()
        keyboardViewBinding!!.emojiSearchView.clearFocus()
//        setupEmojis()

        keyboardViewBinding!!.emojiPickerView.setOnEmojiPickedListener{
            mOnKeyboardActionListener?.onText(it.emoji)
        }
        onEmojiSearchViewOperation()
    }

    private fun closeEmojiPalette() {
        keyboardViewBinding?.apply {
            emojiPaletteHolder.beGone()
//            emojisList.scrollToPosition(0)
            suggestionsHolder.beVisible()
        }
    }

//    private fun setupEmojis() {
//        ensureBackgroundThread {
//            val fullEmojiList = parseRawEmojiSpecsFile(context, EMOJI_SPEC_FILE_PATH)
//            val systemFontPaint = Paint().apply {
//                typeface = Typeface.DEFAULT
//            }
//
//            val emojis = fullEmojiList.filter { emoji ->
//                systemFontPaint.hasGlyph(emoji.emoji) || (EmojiCompat.get().loadState == EmojiCompat.LOAD_STATE_SUCCEEDED && EmojiCompat.get()
//                    .getEmojiMatch(emoji.emoji, emojiCompatMetadataVersion) == EMOJI_SUPPORTED)
//            }
//
//            Handler(Looper.getMainLooper()).post {
//                setupEmojiAdapter(emojis)
//            }
//        }
//    }

    // For Vietnamese - Telex
    private fun setupLanguageTelex() {
        ensureBackgroundThread {
            parseRawJsonSpecsFile(context, LANGUAGE_VN_TELEX)
        }
    }

//    private fun prepareEmojiCategories(emojis: List<EmojiData>): Map<String, List<EmojiData>> {
//        val recentEmojis = context.config.recentlyUsedEmojis
//            .mapNotNull { emoji ->
//                val emojiData = emojis.firstOrNull { it.emoji == emoji }
//                emojiData?.copy(category = RECENTLY_USED_EMOJIS)
//            }
//
//        return (recentEmojis + emojis).groupBy { it.category }
//    }
//
//    private fun prepareEmojiItems(categories: Map<String, List<EmojiData>>): List<EmojisAdapter.Item> {
//        val emojiItems = mutableListOf<EmojisAdapter.Item>()
//        categories.entries.forEach { (category, emojis) ->
//            emojiItems.add(EmojisAdapter.Item.Category(category))
//            emojiItems.addAll(emojis.map(EmojisAdapter.Item::Emoji))
//        }
//
//        return emojiItems
//    }
//
//    private fun setupEmojiAdapter(emojis: List<EmojiData>) {
//        val emojiCategories = prepareEmojiCategories(emojis)
//        var emojiItems = prepareEmojiItems(emojiCategories)
//
//        val emojiLayoutManager = AutoGridLayoutManager(
//            context = context,
//            itemWidth = context.resources.getDimensionPixelSize(R.dimen.emoji_item_size)
//        ).apply {
//            spanSizeLookup = object : SpanSizeLookup() {
//                override fun getSpanSize(position: Int): Int {
//                    return if (emojiItems[position] is EmojisAdapter.Item.Category) {
//                        spanCount
//                    } else {
//                        1
//                    }
//                }
//            }
//        }
//
//        val emojiCategoryIds = mutableMapOf<Int, String>()
//        val emojiCategoryColor = mTextColor.adjustAlpha(0.8f)
//        keyboardViewBinding?.emojiCategoriesStrip?.apply {
//            weightSum = emojiCategories.count().toFloat()
//            val strip = this
//            removeAllViews()
//            emojiCategories.entries.forEach { (category, _) ->
//                ItemEmojiCategoryBinding.inflate(
//                    LayoutInflater.from(context),
//                    this,
//                    true
//                ).root.apply {
//                    id = generateViewId()
//                    emojiCategoryIds[id] = category
//                    setImageResource(getCategoryIconRes(category))
//                    layoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.MATCH_PARENT,
//                        LinearLayout.LayoutParams.MATCH_PARENT,
//                        1f
//                    )
//                    setOnClickListener {
//                        strip.children.filterIsInstance<ImageButton>().forEach {
//                            it.applyColorFilter(emojiCategoryColor)
//                        }
//                        applyColorFilter(mPrimaryColor)
//                        keyboardViewBinding?.emojisList?.stopScroll()
//                        emojiLayoutManager.scrollToPositionWithOffset(
//                            emojiItems.indexOfFirst { it is EmojisAdapter.Item.Category && it.value == category },
//                            0
//                        )
//                    }
//                    applyColorFilter(emojiCategoryColor)
//                }
//            }
//        }
//
//        keyboardViewBinding?.emojisList?.apply {
//            layoutManager = emojiLayoutManager
//            adapter = EmojisAdapter(context = safeStorageContext, items = emojiItems) { emoji ->
//                mOnKeyboardActionListener!!.onText(emoji.emoji)
//                vibrateIfNeeded()
//
//                context.config.addRecentEmoji(emoji.emoji)
//                (adapter as? EmojisAdapter)?.apply {
//                    emojiItems = prepareEmojiItems(prepareEmojiCategories(emojis))
//                    updateItems(emojiItems)
//                }
//            }
//
//            clearOnScrollListeners()
//            onScroll { offset ->
//                keyboardViewBinding!!.emojiPaletteTopBar.elevation = when {
//                    offset > 4 -> context.resources.getDimensionPixelSize(R.dimen.one_dp).toFloat()
//                    else -> 0f
//                }
//
//                emojiLayoutManager.findFirstCompletelyVisibleItemPosition()
//                    .also { firstVisibleIndex ->
//                        emojiItems
//                            .withIndex()
//                            .lastOrNull { it.value is EmojisAdapter.Item.Category && it.index <= firstVisibleIndex }
//                            ?.also { activeCategory ->
//                                val id = emojiCategoryIds.entries.first {
//                                    it.value == (activeCategory.value as EmojisAdapter.Item.Category).value
//                                }.key
//
//                                keyboardViewBinding
//                                    ?.emojiCategoriesStrip
//                                    ?.children
//                                    ?.filterIsInstance<ImageButton>()
//                                    ?.forEach { button ->
//                                        val selected = button.id == id
//                                        button.applyColorFilter(
//                                            if (selected) mPrimaryColor else emojiCategoryColor
//                                        )
//                                    }
//                            }
//                    }
//            }
//        }
//    }

    private fun closing() {
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
        removeMessages()
        dismissPopupKeyboard()
        mBuffer = null
        mCanvas = null
        mMiniKeyboardCache.clear()
    }

    private fun removeMessages() {
        mHandler?.apply {
            removeMessages(MSG_REPEAT)
            removeMessages(MSG_LONGPRESS)
        }
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    private fun dismissPopupKeyboard() {
        if (mPopupKeyboard.isShowing) {
            mPopupKeyboard.dismiss()
            mMiniKeyboardOnScreen = false
            setCurrentKeyPressed(false)
            invalidateAllKeys()
        }
    }

    private fun getKeyColor(): Int {
        val backgroundColor = safeStorageContext.getKeyboardBackgroundColor()
        val lighterColor = backgroundColor.lightenColor()
        val keyColor = if (safeStorageContext.isDynamicTheme()) {
            lighterColor
        } else {
            if (backgroundColor == Color.BLACK) {
                backgroundColor.getContrastColor().adjustAlpha(0.1f)
            } else {
                lighterColor
            }
        }
        return keyColor
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun addToClipboardViews(it: InlineContentView, addToFront: Boolean = false) {
        if (keyboardViewBinding?.autofillSuggestionsHolder != null) {
            val newLayoutParams = LinearLayout.LayoutParams(it.layoutParams)
            newLayoutParams.updateMarginsRelative(start = resources.getDimensionPixelSize(R.dimen.normal_margin))
            it.layoutParams = newLayoutParams
            if (addToFront) {
                keyboardViewBinding?.autofillSuggestionsHolder?.addView(it, 0)
            } else {
                keyboardViewBinding?.autofillSuggestionsHolder?.addView(it)
            }
            updateSuggestionsToolbarLayout()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clearClipboardViews() {
        keyboardViewBinding?.autofillSuggestionsHolder?.removeAllViews()
        updateSuggestionsToolbarLayout()
    }

    private fun updateSuggestionsToolbarLayout() {
        keyboardViewBinding?.apply {
            if (hasInlineViews()) {
                // make room on suggestion toolbar for inline views
                suggestionsItemsHolder.gravity = Gravity.NO_GRAVITY
                clipboardValue.maxWidth =
                    resources.getDimensionPixelSize(R.dimen.suggestion_max_width)
            } else {
                // restore original clipboard toolbar appearance
                suggestionsItemsHolder.gravity = Gravity.CENTER_HORIZONTAL
                suggestionsHolder.measuredWidth.also { maxWidth ->
                    clipboardValue.maxWidth = maxWidth
                }
            }
        }
    }

    /**
     * Returns true if there are [InlineContentView]s in [autofill_suggestions_holder]
     */
    private fun hasInlineViews() =
        (keyboardViewBinding?.autofillSuggestionsHolder?.childCount ?: 0) > 0

    /**
     * Returns: Popup Key width depends on popup keys count
     */
    private fun MyKeyboard.Key.calcKeyWidth(containerWidth: Int): Int {
        val popupKeyCount = this.popupCharacters!!.length

        return if (popupKeyCount > containerWidth / this.width) {
            containerWidth / popupKeyCount
        } else {
            this.width
        }
    }
}
