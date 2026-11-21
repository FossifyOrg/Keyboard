package org.fossify.keyboard.activities

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
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
import org.fossify.keyboard.helpers.DEFAULT_DICTIONARY_LANG
import org.fossify.keyboard.helpers.DICTIONNARY_EXTRA_LANG
import org.fossify.keyboard.helpers.DictionnaryClient
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_70_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_80_PERCENT
import org.fossify.keyboard.helpers.KEYBOARD_HEIGHT_90_PERCENT
import org.fossify.keyboard.helpers.SOUND_ALWAYS
import org.fossify.keyboard.helpers.SOUND_NONE
import org.fossify.keyboard.helpers.SOUND_SYSTEM
import org.fossify.keyboard.helpers.dictionaryLangForKeyboardLanguage
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    companion object {
        private const val REQUEST_DOWNLOAD_DICTIONARY = 1001
        private val REQUEST_PICK_DICTIONNARY_APK = DictionnaryClient.REQUEST_PICK_DICTIONNARY_APK
    }

    private var installProposalShown = false

    // languages a download was already attempted for during this settings visit, so a failing
    // download is not relaunched in a loop from onActivityResult
    private val dictionaryDownloadAttempts = mutableSetOf<String>()

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
        setupSoundOnKeypress()
        setupShowPopupOnKeypress()
        setupShowKeyBorders()
        setupManageKeyboardLanguages()
        setupKeyboardLanguage()
        setupKeyboardHeightMultiplier()
        setupShowEmojiKey()
        setupShowLanguageSwitchKey()
        setupShowClipboardContent()
        setupSentencesCapitalization()
        setupShowNumbersRow()
        setupShowSuggestions()
        setupAutoCorrect()
        setupDictionnaryStatus()
        setupVoiceInputMethod()
        maybeProposeDictionnaryInstall()

        binding.apply {
            updateTextColors(settingsNestedScrollview)

            arrayOf(
                settingsColorCustomizationSectionLabel,
                settingsGeneralSettingsLabel,
                settingsLayoutAppearanceLabel,
                settingsKeypressLabel,
                settingsTypingInputLabel,
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

    private fun setupSoundOnKeypress() {
        binding.apply {
            settingsSoundOnKeypress.text = getSoundOnKeypressText(config.soundOnKeypress)
            settingsSoundOnKeypressHolder.setOnClickListener {
                val items = arrayListOf(
                    RadioItem(SOUND_NONE, getString(R.string.sound_none)),
                    RadioItem(SOUND_SYSTEM, getString(R.string.sound_system)),
                    RadioItem(SOUND_ALWAYS, getString(R.string.sound_always))
                )
                RadioGroupDialog(
                    activity = this@SettingsActivity,
                    items = items,
                    checkedItemId = config.soundOnKeypress
                ) {
                    config.soundOnKeypress = it as Int
                    settingsSoundOnKeypress.text = getSoundOnKeypressText(config.soundOnKeypress)
                }
            }
        }
    }

    private fun getSoundOnKeypressText(mode: Int): String = getString(
        when (mode) {
            SOUND_SYSTEM -> R.string.sound_system
            SOUND_ALWAYS -> R.string.sound_always
            else -> R.string.sound_none
        }
    )

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
                    ensureDictionariesForSelectedLanguages()
                }
            }
        }
    }

    /**
     * For every selected keyboard language that has a dictionary but no local copy yet,
     * launches the Dictionnary app to download it (or proposes to install that app).
     */
    private fun ensureDictionariesForSelectedLanguages() {
        val missingLangs = config.selectedLanguages
            .mapNotNull { dictionaryLangForKeyboardLanguage(it) }
            .distinct()
            .filter { !DictionnaryClient.isLocalDictionaryAvailable(this, it) }
        if (missingLangs.isNotEmpty()) {
            ensureDictionary(missingLangs.first())
        }
    }

    private fun ensureDictionaryForCurrentLanguage() {
        val lang = dictionaryLangForKeyboardLanguage(config.keyboardLanguage) ?: return
        if (!DictionnaryClient.isLocalDictionaryAvailable(this, lang)) {
            ensureDictionary(lang)
        }
    }

    private fun ensureDictionary(lang: String) {
        if (!dictionaryDownloadAttempts.add(lang)) {
            return
        }
        if (DictionnaryClient.isDictionnaryInstalled(this)) {
            DictionnaryClient.launchDictionaryDownload(this, lang, REQUEST_DOWNLOAD_DICTIONARY)
        } else {
            DictionnaryClient.proposeInstallDictionnary(this, lang, REQUEST_PICK_DICTIONNARY_APK)
        }
    }

    /** settings row showing whether the Dictionnary app is installed, tap to install/open it */
    private fun setupDictionnaryStatus() {
        binding.apply {
            val installed = DictionnaryClient.isDictionnaryInstalled(this@SettingsActivity)
            settingsDictionnaryValue.text = getString(
                if (installed) R.string.dictionnary_installed else R.string.dictionnary_not_installed
            )
            settingsDictionnaryHolder.setOnClickListener {
                val lang = dictionaryLangForKeyboardLanguage(config.keyboardLanguage)
                when {
                    !installed -> {
                        if (lang != null) {
                            DictionnaryClient.proposeInstallDictionnary(this@SettingsActivity, lang, REQUEST_PICK_DICTIONNARY_APK)
                        } else {
                            DictionnaryClient.proposeInstallDictionnary(this@SettingsActivity, DEFAULT_DICTIONARY_LANG, REQUEST_PICK_DICTIONNARY_APK)
                        }
                    }

                    lang != null && !DictionnaryClient.isLocalDictionaryAvailable(this@SettingsActivity, lang) ->
                        ensureDictionary(lang)

                    else -> DictionnaryClient.openDictionnaryApp(this@SettingsActivity)
                }
            }
        }
    }

    /**
     * If suggestions or auto-correct are enabled but the dictionary is missing and the
     * Dictionnary app is not installed, propose to install it (once per settings visit).
     */
    private fun maybeProposeDictionnaryInstall() {
        if (installProposalShown) {
            return
        }
        if (!config.showSuggestions && !config.autoCorrectOnSpace) {
            return
        }
        val lang = dictionaryLangForKeyboardLanguage(config.keyboardLanguage) ?: return
        if (DictionnaryClient.isLocalDictionaryAvailable(this, lang)) {
            return
        }
        if (DictionnaryClient.isDictionnaryInstalled(this)) {
            return
        }
        installProposalShown = true
        DictionnaryClient.proposeInstallDictionnary(this, lang, REQUEST_PICK_DICTIONNARY_APK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_DOWNLOAD_DICTIONARY) {
            val lang = resultData?.getStringExtra(DICTIONNARY_EXTRA_LANG)
            val file = DictionnaryClient.handleDownloadResult(this, resultCode, resultData)
            if (file != null) {
                toast(getString(R.string.dictionary_downloaded_ok, lang.orEmpty()))
            } else if (lang != null) {
                toast(getString(R.string.dictionary_download_failed, lang))
            }
            // continue with the other missing languages, if any
            ensureDictionariesForSelectedLanguages()
        } else if (requestCode == REQUEST_PICK_DICTIONNARY_APK && resultCode == RESULT_OK) {
            resultData?.data?.let {
                // hand the picked APK to the system package installer
                DictionnaryClient.installApkFromUri(this, it)
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

    private fun setupShowEmojiKey() {
        binding.apply {
            settingsShowEmojiKeyHolder.setOnClickListener {
                settingsShowEmojiKey.toggle()
                config.showEmojiKey = settingsShowEmojiKey.isChecked
                if (settingsShowEmojiKey.isChecked) {
                    config.showLanguageSwitchKey = false
                    settingsShowLanguageSwitchKey.isChecked = false
                }
            }
            settingsShowEmojiKey.isChecked = config.showEmojiKey
        }
    }

    private fun setupShowLanguageSwitchKey() {
        binding.apply {
            settingsShowLanguageSwitchKeyHolder.setOnClickListener {
                settingsShowLanguageSwitchKey.toggle()
                config.showLanguageSwitchKey = settingsShowLanguageSwitchKey.isChecked
                if (settingsShowLanguageSwitchKey.isChecked) {
                    config.showEmojiKey = false
                    settingsShowEmojiKey.isChecked = false
                }
            }
            settingsShowLanguageSwitchKey.isChecked = config.showLanguageSwitchKey
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

    private fun setupShowSuggestions() {
        binding.apply {
            settingsShowSuggestions.isChecked = config.showSuggestions
            settingsShowSuggestionsHolder.setOnClickListener {
                settingsShowSuggestions.toggle()
                config.showSuggestions = settingsShowSuggestions.isChecked
                if (settingsShowSuggestions.isChecked) {
                    ensureDictionaryForCurrentLanguage()
                }
            }
        }
    }

    private fun setupAutoCorrect() {
        binding.apply {
            settingsAutoCorrect.isChecked = config.autoCorrectOnSpace
            settingsAutoCorrectHolder.setOnClickListener {
                settingsAutoCorrect.toggle()
                config.autoCorrectOnSpace = settingsAutoCorrect.isChecked
                if (settingsAutoCorrect.isChecked) {
                    ensureDictionaryForCurrentLanguage()
                }
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
