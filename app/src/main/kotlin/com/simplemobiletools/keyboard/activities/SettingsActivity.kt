package com.simplemobiletools.keyboard.activities

import android.content.Intent
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.isTiramisuPlus
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.keyboard.databinding.ActivitySettingsBinding
import com.simplemobiletools.keyboard.extensions.config
import com.simplemobiletools.keyboard.extensions.getKeyboardLanguageText
import com.simplemobiletools.keyboard.extensions.getKeyboardLanguages
import com.simplemobiletools.keyboard.helpers.*
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            updateMaterialActivityViews(settingsCoordinator, settingsHolder, useTransparentNavigation = true, useTopSearchMenu = false)
            setupMaterialScrollListener(settingsNestedScrollview, settingsToolbar)
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupPurchaseThankYou()
        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupManageClipboardItems()
        setupVibrateOnKeypress()
        setupShowPopupOnKeypress()
        setupShowKeyBorders()
        setupKeyboardLanguage()
        setupKeyboardHeightMultiplier()
        setupShowClipboardContent()
        setupSentencesCapitalization()
        setupShowNumbersRow()
        setupShowVoiceButton()

        binding.apply {
            updateTextColors(settingsNestedScrollview)

            arrayOf(settingsColorCustomizationSectionLabel, settingsGeneralSettingsLabel).forEach {
                it.setTextColor(getProperPrimaryColor())
            }
        }
    }

    private fun setupPurchaseThankYou() {
        binding.apply {
            settingsPurchaseThankYouHolder.beGoneIf(isOrWasThankYouInstalled())
            settingsPurchaseThankYouHolder.setOnClickListener {
                launchPurchaseThankYouIntent()
            }
        }
    }

    private fun setupCustomizeColors() {
        binding.apply {
            settingsColorCustomizationLabel.text = getCustomizeColorsString()
            settingsColorCustomizationHolder.setOnClickListener {
                handleCustomizeColorsClick()
            }
        }
    }

    private fun setupUseEnglish() {
        binding.apply {
            settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
            settingsUseEnglish.isChecked = config.useEnglish
            settingsUseEnglishHolder.setOnClickListener {
                settingsUseEnglish.toggle()
                config.useEnglish = settingsUseEnglish.isChecked
                exitProcess(0)
            }
        }
    }

    private fun setupLanguage() {
        binding.apply {
            settingsLanguage.text = Locale.getDefault().displayLanguage
            settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
            settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        }
    }

    private fun setupManageClipboardItems() {
        binding.settingsManageClipboardItemsHolder.setOnClickListener {
            Intent(this, ManageClipboardItemsActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    private fun setupVibrateOnKeypress() {
        binding.apply {
            settingsVibrateOnKeypress.isChecked = config.vibrateOnKeypress
            settingsVibrateOnKeypressHolder.setOnClickListener {
                settingsVibrateOnKeypress.toggle()
                config.vibrateOnKeypress = settingsVibrateOnKeypress.isChecked
            }
        }
    }

    private fun setupShowPopupOnKeypress() {
        binding.apply {
            settingsShowPopupOnKeypress.isChecked = config.showPopupOnKeypress
            settingsShowPopupOnKeypressHolder.setOnClickListener {
                settingsShowPopupOnKeypress.toggle()
                config.showPopupOnKeypress = settingsShowPopupOnKeypress.isChecked
            }
        }
    }

    private fun setupShowKeyBorders() {
        binding.apply {
            settingsShowKeyBorders.isChecked = config.showKeyBorders
            settingsShowKeyBordersHolder.setOnClickListener {
                settingsShowKeyBorders.toggle()
                config.showKeyBorders = settingsShowKeyBorders.isChecked
            }
        }
    }

    private fun setupKeyboardLanguage() {
        binding.apply {
            settingsKeyboardLanguage.text = getKeyboardLanguageText(config.keyboardLanguage)
            settingsKeyboardLanguageHolder.setOnClickListener {
                val items = getKeyboardLanguages()
                RadioGroupDialog(this@SettingsActivity, items, config.keyboardLanguage) {
                    config.keyboardLanguage = it as Int
                    settingsKeyboardLanguage.text = getKeyboardLanguageText(config.keyboardLanguage)
                }
            }
        }
    }

    private fun setupKeyboardHeightMultiplier() {
        binding.apply {
            settingsKeyboardHeightMultiplier.text = getKeyboardHeightPercentageText(config.keyboardHeightPercentage)
            settingsKeyboardHeightMultiplierHolder.setOnClickListener {
                val items = arrayListOf(
                    RadioItem(KEYBOARD_HEIGHT_70_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_70_PERCENT)),
                    RadioItem(KEYBOARD_HEIGHT_80_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_80_PERCENT)),
                    RadioItem(KEYBOARD_HEIGHT_90_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_90_PERCENT)),
                    RadioItem(KEYBOARD_HEIGHT_100_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_100_PERCENT)),
                    RadioItem(KEYBOARD_HEIGHT_120_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_120_PERCENT)),
                    RadioItem(KEYBOARD_HEIGHT_140_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_140_PERCENT)),
                    RadioItem(KEYBOARD_HEIGHT_160_PERCENT, getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_160_PERCENT)),
                )

                RadioGroupDialog(this@SettingsActivity, items, config.keyboardHeightPercentage) {
                    config.keyboardHeightPercentage = it as Int
                    settingsKeyboardHeightMultiplier.text = getKeyboardHeightPercentageText(config.keyboardHeightPercentage)
                }
            }
        }
    }

    private fun getKeyboardHeightPercentageText(keyboardHeightPercentage: Int): String = "$keyboardHeightPercentage%"

    private fun setupShowClipboardContent() {
        binding.apply {
            settingsShowClipboardContent.isChecked = config.showClipboardContent
            settingsShowClipboardContentHolder.setOnClickListener {
                settingsShowClipboardContent.toggle()
                config.showClipboardContent = settingsShowClipboardContent.isChecked
            }
        }
    }

    private fun setupSentencesCapitalization() {
        binding.apply {
            settingsStartSentencesCapitalized.isChecked = config.enableSentencesCapitalization
            settingsStartSentencesCapitalizedHolder.setOnClickListener {
                settingsStartSentencesCapitalized.toggle()
                config.enableSentencesCapitalization = settingsStartSentencesCapitalized.isChecked
            }
        }
    }

    private fun setupShowNumbersRow() {
        binding.apply {
            settingsShowNumbersRow.isChecked = config.showNumbersRow
            settingsShowNumbersRowHolder.setOnClickListener {
                settingsShowNumbersRow.toggle()
                config.showNumbersRow = settingsShowNumbersRow.isChecked
            }
        }
    }

    private fun setupShowVoiceButton() {
        binding.apply {
            settingsShowVoiceButton.isChecked = config.showVoiceButton
            settingsShowVoiceButtonHolder.setOnClickListener {
                settingsShowVoiceButton.toggle()
                config.showVoiceButton = settingsShowVoiceButton.isChecked
            }
        }
    }
}
