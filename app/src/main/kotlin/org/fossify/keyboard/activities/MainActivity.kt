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
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.LICENSE_GSON
import org.fossify.commons.models.FAQItem
import org.fossify.keyboard.BuildConfig
import org.fossify.keyboard.R
import org.fossify.keyboard.databinding.ActivityMainBinding
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.inputMethodManager
import org.fossify.keyboard.helpers.DICTIONNARY_EXTRA_LANG
import org.fossify.keyboard.helpers.DictionnaryClient
import org.fossify.keyboard.helpers.dictionaryLangForKeyboardLanguage

class MainActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)

    companion object {
        private const val REQUEST_DOWNLOAD_DICTIONARY = 1001
        private val REQUEST_PICK_DICTIONNARY_APK = DictionnaryClient.REQUEST_PICK_DICTIONNARY_APK
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        binding.apply {
            setupEdgeToEdge(padBottomSystem = listOf(mainNestedScrollview))
            setupMaterialScrollListener(binding.mainNestedScrollview, binding.mainAppbar)

            changeKeyboardHolder.setOnClickListener {
                inputMethodManager.showInputMethodPicker()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.mainAppbar)
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
        maybeProposeDictionaryLanguage()

        updateTextColors(binding.mainNestedScrollview)
        updateChangeKeyboardColor()
    }

    /**
     * At launch, in addition to asking to add the keyboard to the enabled input methods,
     * ask the user to install the dictionary of the current keyboard language: launch the
     * Dictionnary app's download flow when it is installed, otherwise propose to install
     * that app. Asked only once, and again later if the dictionary is missing again.
     */
    private fun maybeProposeDictionaryLanguage() {
        if (!config.showSuggestions && !config.autoCorrectOnSpace) {
            return
        }
        val lang = dictionaryLangForKeyboardLanguage(config.keyboardLanguage) ?: return
        if (DictionnaryClient.isLocalDictionaryAvailable(this, lang)) {
            config.wasDictionaryLanguageProposed = false
            return
        }
        if (config.wasDictionaryLanguageProposed) {
            return
        }
        config.wasDictionaryLanguageProposed = true
        if (DictionnaryClient.isDictionnaryInstalled(this)) {
            DictionnaryClient.launchDictionaryDownload(this, lang, REQUEST_DOWNLOAD_DICTIONARY)
        } else {
            DictionnaryClient.proposeInstallDictionnary(this, lang, REQUEST_PICK_DICTIONNARY_APK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        when (requestCode) {
            REQUEST_DOWNLOAD_DICTIONARY -> {
                val lang = resultData?.getStringExtra(DICTIONNARY_EXTRA_LANG)
                val file = DictionnaryClient.handleDownloadResult(this, resultCode, resultData)
                if (file != null) {
                    toast(getString(R.string.dictionary_downloaded_ok, lang.orEmpty()))
                } else if (lang != null) {
                    toast(getString(R.string.dictionary_download_failed, lang))
                }
            }

            REQUEST_PICK_DICTIONNARY_APK -> {
                if (resultCode == RESULT_OK) {
                    resultData?.data?.let { DictionnaryClient.installApkFromUri(this, it) }
                }
            }
        }
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
                resources.getBoolean(R.bool.is_google_play_build)
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_GSON

        val faqItems = ArrayList<FAQItem>()
        if (resources.getBoolean(R.bool.is_google_play_build)) {
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
