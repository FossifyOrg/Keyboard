package org.fossify.keyboard.activities

import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.provider.Settings
import org.fossify.commons.dialogs.ConfirmationAdvancedDialog
import org.fossify.commons.extensions.appLaunched
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.launchMoreAppsFromUsIntent
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.LICENSE_GSON
import org.fossify.commons.models.FAQItem
import org.fossify.keyboard.BuildConfig
import org.fossify.keyboard.R
import org.fossify.keyboard.databinding.ActivityMainBinding
import org.fossify.keyboard.extensions.inputMethodManager

class MainActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        binding.apply {
            updateMaterialActivityViews(
                mainCoordinator,
                mainHolder,
                useTransparentNavigation = false,
                useTopSearchMenu = false
            )
            setupMaterialScrollListener(mainNestedScrollview, mainToolbar)

            changeKeyboardHolder.setOnClickListener {
                inputMethodManager.showInputMethodPicker()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.mainToolbar)
        if (!isKeyboardEnabled()) {
            ConfirmationAdvancedDialog(
                activity = this,
                messageId = R.string.redirection_note,
                positive = R.string.ok,
                negative = 0
            ) { success ->
                if (success) {
                    Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(this)
                    }
                } else {
                    finish()
                }
            }
        }

        updateTextColors(binding.mainNestedScrollview)
        updateChangeKeyboardColor()
    }

    private fun setupOptionsMenu() {
        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        binding.mainToolbar.menu.apply {
            findItem(R.id.more_apps_from_us).isVisible =
                !resources.getBoolean(R.bool.hide_google_relations)
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_GSON

        val faqItems = ArrayList<FAQItem>()
        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))
            faqItems.add(FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons))
        }

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    private fun updateChangeKeyboardColor() {
        val applyBackground =
            resources.getDrawable(R.drawable.button_background_rounded, theme) as RippleDrawable
        (applyBackground as LayerDrawable).findDrawableByLayerId(R.id.button_background_holder)
            .applyColorFilter(getProperPrimaryColor())
        binding.changeKeyboard.apply {
            background = applyBackground
            setTextColor(getProperPrimaryColor().getContrastColor())
        }
    }

    private fun isKeyboardEnabled(): Boolean {
        return inputMethodManager.enabledInputMethodList.any {
            it.settingsActivity == SettingsActivity::class.java.canonicalName
        }
    }
}
