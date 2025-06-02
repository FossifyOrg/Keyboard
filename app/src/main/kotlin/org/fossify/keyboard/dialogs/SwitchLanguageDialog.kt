package org.fossify.keyboard.dialogs

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.databinding.DialogRadioGroupBinding
import org.fossify.commons.databinding.RadioButtonBinding
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.keyboard.R
import org.fossify.keyboard.activities.SettingsActivity
import org.fossify.keyboard.extensions.*

class SwitchLanguageDialog(
    private val inputView: View,
    private val callback: () -> Unit,
) {
    private val context = ContextThemeWrapper(inputView.context.safeStorageContext, R.style.MyKeyboard_Alert)
    private val config = context.config
    private val items = context.getKeyboardLanguagesRadioItems()
    private val layoutInflater = LayoutInflater.from(context)

    private var dialog: AlertDialog? = null
    private var wasInit = false
    private var selectedItemId = -1

    init {
        val binding = DialogRadioGroupBinding.inflate(layoutInflater)
        val checkedItemId = config.keyboardLanguage
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

        context.getKeyboardDialogBuilder().apply {
            setPositiveButton(R.string.manage_keyboard_languages) { _, _ ->
                Intent(context, SettingsActivity::class.java).apply {
                    flags = FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(this)
                }
            }

            context.setupKeyboardDialogStuff(inputView.windowToken, binding.root, this) { alertDialog ->
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
            config.keyboardLanguage = items[checkedId].value as Int
            callback()
            dialog?.dismiss()
        }
    }
}
