package com.rishabh.emojipicker



import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import kotlin.math.roundToInt

/**
 * Default controller class for emoji picker popup view.
 *
 * <p>Shows the popup view above the target Emoji. View under control is a {@code
 * EmojiPickerPopupView}.
 */
internal class EmojiPickerPopupViewController(
    private val context: Context,
    private val emojiPickerPopupView: EmojiPickerPopupView,
    private val clickedEmojiView: View
) {
    private val popupWindow: PopupWindow =
        PopupWindow(
            emojiPickerPopupView,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            /* focusable= */ false
        )

    fun show() {
        popupWindow.apply {
            val location = IntArray(2)
            clickedEmojiView.getLocationInWindow(location)
            // Make the popup view center align with the target emoji view.
            val x =
                location[0] + clickedEmojiView.width / 2f -
                    emojiPickerPopupView.getPopupViewWidth() / 2f
            val y = location[1] - emojiPickerPopupView.getPopupViewHeight()
            // Set background drawable so that the popup window is dismissed properly when clicking
            // outside / scrolling for API < 23.
            setBackgroundDrawable(context.getDrawable(R.drawable.popup_view_rounded_background))
            isOutsideTouchable = true
            isTouchable = true
            animationStyle = R.style.VariantPopupAnimation
            elevation =
                clickedEmojiView.context.resources
                    .getDimensionPixelSize(R.dimen.emoji_picker_popup_view_elevation)
                    .toFloat()
            try {
                showAtLocation(clickedEmojiView, Gravity.NO_GRAVITY, x.roundToInt(), y)
            } catch (e: WindowManager.BadTokenException) {
                Toast.makeText(
                        context,
                        "Don't use EmojiPickerView inside a Popup",
                        Toast.LENGTH_LONG
                    )
                    .show()
            }
        }
    }

    fun dismiss() {
        if (popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }
}
