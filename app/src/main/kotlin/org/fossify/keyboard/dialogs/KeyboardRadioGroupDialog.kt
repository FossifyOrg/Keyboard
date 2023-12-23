package org.fossify.keyboard.dialogs

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.databinding.DialogRadioGroupBinding
import org.fossify.commons.databinding.RadioButtonBinding
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.commons.models.RadioItem
import org.fossify.keyboard.R
import org.fossify.keyboard.extensions.getKeyboardDialogBuilder
import org.fossify.keyboard.extensions.safeStorageContext
import org.fossify.keyboard.extensions.setupKeyboardDialogStuff

class KeyboardRadioGroupDialog(
    private val inputView: View,
    private val items: ArrayList<RadioItem>,
    private val checkedItemId: Int = -1,
    private val titleId: Int = 0,
    showOKButton: Boolean = false,
    private val cancelCallback: (() -> Unit)? = null,
    private val callback: (newValue: Any) -> Unit
) {
    private val context = ContextThemeWrapper(inputView.context.safeStorageContext, R.style.MyKeyboard_Alert)
    private var dialog: AlertDialog? = null
    private var wasInit = false
    private var selectedItemId = -1
    private val layoutInflater = LayoutInflater.from(context)

    init {
        val binding = DialogRadioGroupBinding.inflate(layoutInflater)
        binding.dialogRadioGroup.apply {
            for (i in 0 until items.size) {
                val radioButton = RadioButtonBinding.inflate(layoutInflater).dialogRadioButton.apply {
                    text = items[i].title
                    isChecked = items[i].id == checkedItemId
                    id = i
                    setOnClickListener { itemSelected(i) }
                }

                if (items[i].id == checkedItemId) {
                    selectedItemId = i
                }

                addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }

        val builder = context.getKeyboardDialogBuilder()
            .setOnCancelListener { cancelCallback?.invoke() }

        if (selectedItemId != -1 && showOKButton) {
            builder.setPositiveButton(R.string.ok) { _, _ -> itemSelected(selectedItemId) }
        }

        builder.apply {
            context.setupKeyboardDialogStuff(inputView.windowToken, binding.root, this, titleId) { alertDialog ->
                dialog = alertDialog
            }
        }

        if (selectedItemId != -1) {
            binding.dialogRadioHolder.apply {
                onGlobalLayout {
                    scrollY = binding.dialogRadioGroup.findViewById<View>(selectedItemId).bottom - height
                }
            }
        }

        wasInit = true
    }

    private fun itemSelected(checkedId: Int) {
        if (wasInit) {
            callback(items[checkedId].value)
            dialog?.dismiss()
        }
    }
}
