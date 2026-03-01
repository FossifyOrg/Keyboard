package org.fossify.keyboard.extensions

import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
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
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.isBlackAndWhiteTheme
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.helpers.isNougatPlus
import org.fossify.commons.models.RadioItem
import org.fossify.commons.views.MyTextView
import org.fossify.keyboard.R
import org.fossify.keyboard.databases.ClipsDatabase
import org.fossify.keyboard.helpers.Config
import org.fossify.keyboard.helpers.INPUT_METHOD_SUBTYPE_VOICE
import org.fossify.keyboard.interfaces.ClipsDao

val Context.config: Config get() = Config.newInstance(applicationContext.safeStorageContext)

val Context.safeStorageContext: Context
    get() = if (isDeviceInDirectBootMode) {
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

fun Context.getVoiceInputMethods(
    imm: InputMethodManager = inputMethodManager
): List<Pair<InputMethodInfo, InputMethodSubtype>> {
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
