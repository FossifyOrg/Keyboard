package org.fossify.keyboard.activities

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.isOrWasThankYouInstalled
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.models.RadioItem
import org.fossify.keyboard.R
import org.fossify.keyboard.databinding.ActivitySettingsBinding
import org.fossify.keyboard.dialogs.ManageKeyboardLanguagesDialog
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.getCurrentVoiceInputMethod
import org.fossify.keyboard.extensions.getKeyboardLanguageText
import org.fossify.keyboard.extensions.getKeyboardLanguagesRadioItems
import org.fossify.keyboard.extensions.getVoiceInputMethods
import org.fossify.keyboard.extensions.getVoiceInputRadioItems
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_100_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_120_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_140_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_160_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_70_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_80_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_90_PERCENT
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            setupEdgeToEdge(padBottomSystem = listOf(settingsNestedScrollview))
            setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupManageClipboardItems()
        setupVibrateOnKeypress()
        setupShowPopupOnKeypress()
        setupShowKeyBorders()
        setupManageKeyboardLanguages()
        setupKeyboardLanguage()
        setupKeyboardHeightMultiplier()
        setupShowClipboardContent()
        setupSentencesCapitalization()
        setupShowNumbersRow()
        setupVoiceInputMethod()

        binding.apply {
            updateTextColors(settingsNestedScrollview)

            arrayOf(
                settingsColorCustomizationSectionLabel,
                settingsGeneralSettingsLabel,
                settingsKeyboardSettingsLabel,
                settingsClipboardSettingsLabel
            ).forEach {
                it.setTextColor(getProperPrimaryColor())
            }
        }
    }

    private fun setupCustomizeColors() {
        binding.apply {
            settingsColorCustomizationHolder.setOnClickListener {
                startCustomizationActivity()
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

    private fun setupManageKeyboardLanguages() {
        binding.apply {
            settingsManageKeyboardLanguagesHolder.setOnClickListener {
                ManageKeyboardLanguagesDialog(this@SettingsActivity) {
                    settingsKeyboardLanguage.text = getKeyboardLanguageText(config.keyboardLanguage)
                }
            }
        }
    }

    private fun setupKeyboardLanguage() {
        binding.apply {
            settingsKeyboardLanguage.text = getKeyboardLanguageText(config.keyboardLanguage)
            settingsKeyboardLanguageHolder.setOnClickListener {
                val items = getKeyboardLanguagesRadioItems()
                RadioGroupDialog(this@SettingsActivity, items, config.keyboardLanguage) {
                    config.keyboardLanguage = it as Int
                    settingsKeyboardLanguage.text = getKeyboardLanguageText(config.keyboardLanguage)
                }
            }
        }
    }

    private fun setupKeyboardHeightMultiplier() {
        binding.apply {
            settingsKeyboardHeightMultiplier.text =
                getKeyboardHeightPercentageText(config.keyboardHeightPercentage)
            settingsKeyboardHeightMultiplierHolder.setOnClickListener {
                val items = arrayListOf(
                    RadioItem(
                        id = KEYBOARD_HEIGHT_70_PERCENT,
                        title = getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_70_PERCENT)
                    ),
                    RadioItem(
                        id = KEYBOARD_HEIGHT_80_PERCENT,
                        title = getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_80_PERCENT)
                    ),
                    RadioItem(
                        id = KEYBOARD_HEIGHT_90_PERCENT,
                        title = getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_90_PERCENT)
                    ),
                    RadioItem(
                        id = KEYBOARD_HEIGHT_100_PERCENT,
                        title = getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_100_PERCENT)
                    ),
                    RadioItem(
                        id = KEYBOARD_HEIGHT_120_PERCENT,
                        title = getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_120_PERCENT)
                    ),
                    RadioItem(
                        id = KEYBOARD_HEIGHT_140_PERCENT,
                        title = getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_140_PERCENT)
                    ),
                    RadioItem(
                        id = KEYBOARD_HEIGHT_160_PERCENT,
                        title = getKeyboardHeightPercentageText(KEYBOARD_HEIGHT_160_PERCENT)
                    ),
                )

                RadioGroupDialog(this@SettingsActivity, items, config.keyboardHeightPercentage) {
                    config.keyboardHeightPercentage = it as Int
                    settingsKeyboardHeightMultiplier.text =
                        getKeyboardHeightPercentageText(config.keyboardHeightPercentage)
                }
            }
        }
    }

    private fun getKeyboardHeightPercentageText(keyboardHeightPercentage: Int): String =
        "$keyboardHeightPercentage%"

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

    private fun setupVoiceInputMethod() {
        binding.apply {
            settingsVoiceInputMethodValue.text =
                getCurrentVoiceInputMethod()?.first?.loadLabel(packageManager)
                    ?: getString(R.string.none)
            settingsVoiceInputMethodHolder.setOnClickListener {
                val inputMethods = getVoiceInputMethods()
                if (inputMethods.isEmpty()) {
                    toast(R.string.no_app_found)
                    return@setOnClickListener
                }

                RadioGroupDialog(
                    activity = this@SettingsActivity,
                    items = getVoiceInputRadioItems(),
                    checkedItemId = inputMethods.indexOf(getCurrentVoiceInputMethod(inputMethods))
                ) {
                    config.voiceInputMethod = inputMethods.getOrNull(it as Int)?.first?.id.orEmpty()
                    settingsVoiceInputMethodValue.text =
                        getCurrentVoiceInputMethod(inputMethods)?.first?.loadLabel(packageManager)
                            ?: getString(R.string.none)
                }
            }
        }
    }
}
