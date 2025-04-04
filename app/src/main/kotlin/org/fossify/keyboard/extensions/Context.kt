package org.fossify.keyboard.extensions

import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.IBinder
import android.os.UserManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.databinding.DialogTitleBinding
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.darkenColor
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.isBlackAndWhiteTheme
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.isSystemInDarkMode
import org.fossify.commons.extensions.lightenColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.helpers.isNougatPlus
import org.fossify.commons.models.RadioItem
import org.fossify.commons.views.MyTextView
import org.fossify.keyboard.R
import org.fossify.keyboard.databases.ClipsDatabase
import org.fossify.keyboard.helpers.Config
import org.fossify.keyboard.helpers.INPUT_METHOD_SUBTYPE_VOICE
import org.fossify.keyboard.helpers.LANGUAGE_ARABIC
import org.fossify.keyboard.helpers.LANGUAGE_BELARUSIAN_CYRL
import org.fossify.keyboard.helpers.LANGUAGE_BELARUSIAN_LATN
import org.fossify.keyboard.helpers.LANGUAGE_BENGALI
import org.fossify.keyboard.helpers.LANGUAGE_BULGARIAN
import org.fossify.keyboard.helpers.LANGUAGE_CENTRAL_KURDISH
import org.fossify.keyboard.helpers.LANGUAGE_CHUVASH
import org.fossify.keyboard.helpers.LANGUAGE_DANISH
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_DVORAK
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_QWERTZ
import org.fossify.keyboard.helpers.LANGUAGE_ESPERANTO
import org.fossify.keyboard.helpers.LANGUAGE_FRENCH_AZERTY
import org.fossify.keyboard.helpers.LANGUAGE_FRENCH_BEPO
import org.fossify.keyboard.helpers.LANGUAGE_GERMAN
import org.fossify.keyboard.helpers.LANGUAGE_GREEK
import org.fossify.keyboard.helpers.LANGUAGE_HEBREW
import org.fossify.keyboard.helpers.LANGUAGE_KABYLE_AZERTY
import org.fossify.keyboard.helpers.LANGUAGE_LITHUANIAN
import org.fossify.keyboard.helpers.LANGUAGE_NORWEGIAN
import org.fossify.keyboard.helpers.LANGUAGE_POLISH
import org.fossify.keyboard.helpers.LANGUAGE_ROMANIAN
import org.fossify.keyboard.helpers.LANGUAGE_RUSSIAN
import org.fossify.keyboard.helpers.LANGUAGE_SLOVENIAN
import org.fossify.keyboard.helpers.LANGUAGE_SPANISH
import org.fossify.keyboard.helpers.LANGUAGE_SWEDISH
import org.fossify.keyboard.helpers.LANGUAGE_TURKISH_Q
import org.fossify.keyboard.helpers.LANGUAGE_UKRAINIAN
import org.fossify.keyboard.helpers.LANGUAGE_VIETNAMESE_TELEX
import org.fossify.keyboard.interfaces.ClipsDao

val Context.config: Config get() = Config.newInstance(applicationContext.safeStorageContext)

val Context.safeStorageContext: Context
    get() = if (isNougatPlus() && isDeviceInDirectBootMode) {
        createDeviceProtectedStorageContext()
    } else {
        this
    }

val Context.isDeviceInDirectBootMode: Boolean
    get() {
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        return isNougatPlus() && !userManager.isUserUnlocked
    }

val Context.isDeviceLocked: Boolean
    get() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked || isDeviceInDirectBootMode
    }

val Context.inputMethodManager: InputMethodManager
    get() = getSystemService(InputMethodService.INPUT_METHOD_SERVICE) as InputMethodManager

val Context.clipsDB: ClipsDao
    get() = ClipsDatabase.getInstance(applicationContext.safeStorageContext).ClipsDao()

fun Context.getCurrentClip(): String? {
    val clipboardManager = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
    return clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
}

fun Context.getKeyboardBackgroundColor(): Int {
    val color = if (isDynamicTheme()) {
        resources.getColor(R.color.you_keyboard_background_color, theme)
    } else {
        getProperBackgroundColor().darkenColor(2)
    }

    // use darker background color when key borders are enabled
    if (config.showKeyBorders) {
        val darkerColor = color.darkenColor(2)
        return if (darkerColor == Color.WHITE) {
            resources.getColor(R.color.md_grey_200, theme)
        } else {
            darkerColor
        }
    }

    return color
}

fun Context.getStrokeColor(): Int {
    return if (isDynamicTheme()) {
        if (isSystemInDarkMode()) {
            resources.getColor(R.color.md_grey_800, theme)
        } else {
            resources.getColor(R.color.md_grey_400, theme)
        }
    } else {
        val lighterColor = safeStorageContext.getProperBackgroundColor().lightenColor()
        if (lighterColor == Color.WHITE || lighterColor == Color.BLACK) {
            resources.getColor(R.color.divider_grey, theme)
        } else {
            lighterColor
        }
    }
}

fun Context.getKeyboardDialogBuilder() = if (safeStorageContext.isDynamicTheme()) {
    MaterialAlertDialogBuilder(this, R.style.MyKeyboard_Alert)
} else {
    AlertDialog.Builder(this, R.style.MyKeyboard_Alert)
}

fun Context.setupKeyboardDialogStuff(
    windowToken: IBinder,
    view: View,
    dialog: AlertDialog.Builder,
    titleId: Int = 0,
    titleText: String = "",
    cancelOnTouchOutside: Boolean = true,
    callback: ((alertDialog: AlertDialog) -> Unit)? = null
) {
    val textColor = getProperTextColor()
    val backgroundColor = getProperBackgroundColor()
    val primaryColor = getProperPrimaryColor()
    if (view is ViewGroup) {
        updateTextColors(view)
    } else if (view is MyTextView) {
        view.setColors(textColor, primaryColor, backgroundColor)
    }

    if (dialog is MaterialAlertDialogBuilder) {
        dialog.create().apply {
            if (titleId != 0) {
                setTitle(titleId)
            } else if (titleText.isNotEmpty()) {
                setTitle(titleText)
            }

            val lp = window?.attributes
            lp?.token = windowToken
            lp?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            setView(view)
            setCancelable(cancelOnTouchOutside)
            show()

            val bgDrawable = when {
                isBlackAndWhiteTheme() -> ResourcesCompat.getDrawable(
                    resources, R.drawable.black_dialog_background, theme
                )

                isDynamicTheme() -> ResourcesCompat.getDrawable(
                    resources, R.drawable.dialog_you_background, theme
                )

                else -> resources.getColoredDrawableWithColor(
                    drawableId = R.drawable.dialog_bg,
                    color = baseConfig.backgroundColor
                )
            }

            window?.setBackgroundDrawable(bgDrawable)
            callback?.invoke(this)
        }
    } else {
        var title: TextView? = null
        if (titleId != 0 || titleText.isNotEmpty()) {
            title =
                DialogTitleBinding
                    .inflate(LayoutInflater.from(this))
                    .dialogTitleTextview.apply {
                        if (titleText.isNotEmpty()) {
                            text = titleText
                        } else {
                            setText(titleId)
                        }
                        setTextColor(textColor)
                    }
        }

        // if we use the same primary and background color, use the text color for dialog confirmation buttons
        val dialogButtonColor = if (primaryColor == baseConfig.backgroundColor) {
            textColor
        } else {
            primaryColor
        }

        dialog.create().apply {
            setView(view)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCustomTitle(title)

            val lp = window?.attributes
            lp?.token = windowToken
            lp?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            setCanceledOnTouchOutside(cancelOnTouchOutside)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(dialogButtonColor)

            val bgDrawable = when {
                isBlackAndWhiteTheme() -> ResourcesCompat.getDrawable(
                    resources, R.drawable.black_dialog_background, theme
                )

                isDynamicTheme() -> ResourcesCompat.getDrawable(
                    resources, R.drawable.dialog_you_background, theme
                )

                else -> resources.getColoredDrawableWithColor(
                    drawableId = R.drawable.dialog_bg,
                    color = baseConfig.backgroundColor
                )
            }

            window?.setBackgroundDrawable(bgDrawable)
            callback?.invoke(this)
        }
    }
}

fun Context.getKeyboardLanguagesRadioItems(): ArrayList<RadioItem> {
    val selectedLanguagesRadioItems = arrayListOf<RadioItem>()

    for (lang in config.selectedLanguages) {
        selectedLanguagesRadioItems.add(RadioItem(lang, getKeyboardLanguageText(lang)))
    }

    return selectedLanguagesRadioItems
}

fun Context.getKeyboardLanguageText(language: Int): String {
    return when (language) {
        LANGUAGE_ARABIC -> getString(R.string.translation_arabic)
        LANGUAGE_BELARUSIAN_CYRL -> "${getString(R.string.translation_belarusian)} (Cyrillic)"
        LANGUAGE_BELARUSIAN_LATN -> "${getString(R.string.translation_belarusian)} (Latin)"
        LANGUAGE_BENGALI -> getString(R.string.translation_bengali)
        LANGUAGE_BULGARIAN -> getString(R.string.translation_bulgarian)
        LANGUAGE_CENTRAL_KURDISH -> getString(R.string.translation_central_kurdish)
        LANGUAGE_CHUVASH -> getString(R.string.translation_chuvash)
        LANGUAGE_DANISH -> getString(R.string.translation_danish)
        LANGUAGE_ENGLISH_DVORAK -> "${getString(R.string.translation_english)} (DVORAK)"
        LANGUAGE_ENGLISH_QWERTZ -> "${getString(R.string.translation_english)} (QWERTZ)"
        LANGUAGE_ESPERANTO -> getString(R.string.translation_esperanto)
        LANGUAGE_FRENCH_AZERTY -> "${getString(R.string.translation_french)} (AZERTY)"
        LANGUAGE_FRENCH_BEPO -> "${getString(R.string.translation_french)} (BEPO)"
        LANGUAGE_GERMAN -> getString(R.string.translation_german)
        LANGUAGE_GREEK -> getString(R.string.translation_greek)
        LANGUAGE_HEBREW -> getString(R.string.translation_hebrew)
        LANGUAGE_KABYLE_AZERTY -> "${getString(R.string.translation_kabyle)} (AZERTY)"
        LANGUAGE_LITHUANIAN -> getString(R.string.translation_lithuanian)
        LANGUAGE_NORWEGIAN -> getString(R.string.translation_norwegian)
        LANGUAGE_POLISH -> getString(R.string.translation_polish)
        LANGUAGE_ROMANIAN -> getString(R.string.translation_romanian)
        LANGUAGE_RUSSIAN -> getString(R.string.translation_russian)
        LANGUAGE_SLOVENIAN -> getString(R.string.translation_slovenian)
        LANGUAGE_SPANISH -> getString(R.string.translation_spanish)
        LANGUAGE_SWEDISH -> getString(R.string.translation_swedish)
        LANGUAGE_TURKISH_Q -> "${getString(R.string.translation_turkish)} (Q)"
        LANGUAGE_UKRAINIAN -> getString(R.string.translation_ukrainian)
        LANGUAGE_VIETNAMESE_TELEX -> "${getString(R.string.translation_vietnamese)} (Telex)"
        else -> "${getString(R.string.translation_english)} (QWERTY)"
    }
}

fun Context.getVoiceInputMethods(imm: InputMethodManager = inputMethodManager): List<Pair<InputMethodInfo, InputMethodSubtype>> {
    return imm.enabledInputMethodList.flatMap { im ->
        imm.getEnabledInputMethodSubtypeList(im, true)
            .filter { it.mode == INPUT_METHOD_SUBTYPE_VOICE }
            .map { im to it }
    }
}

fun Context.getCurrentVoiceInputMethod(
    inputMethods: List<Pair<InputMethodInfo, InputMethodSubtype>> = getVoiceInputMethods()
) = inputMethods.find { it.first.id == config.voiceInputMethod }

fun Context.getVoiceInputRadioItems(
    inputMethods: List<Pair<InputMethodInfo, InputMethodSubtype>> = getVoiceInputMethods()
): ArrayList<RadioItem> {
    val radioItems = arrayListOf(RadioItem(id = -1, title = getString(R.string.none)))
    for ((index, pair) in inputMethods.withIndex()) {
        radioItems += RadioItem(id = index, title = pair.first.loadLabel(packageManager).toString())
    }

    return radioItems
}
