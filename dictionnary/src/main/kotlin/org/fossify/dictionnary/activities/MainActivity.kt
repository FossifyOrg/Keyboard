package org.fossify.dictionnary.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import org.fossify.dictionnary.R
import org.fossify.dictionnary.helpers.ACTION_DOWNLOAD_DICTIONARY
import org.fossify.dictionnary.helpers.AVAILABLE_DICTIONARIES
import org.fossify.dictionnary.helpers.DictionaryDownloader
import org.fossify.dictionnary.helpers.EXTRA_CALLING_PACKAGE
import org.fossify.dictionnary.helpers.EXTRA_ERROR
import org.fossify.dictionnary.helpers.EXTRA_LANG
import org.fossify.dictionnary.helpers.EXTRA_RESULT_URI
import org.fossify.dictionnary.helpers.dictionaryFileUri

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var downloadButton: Button
    private val mainHandler = Handler(Looper.getMainLooper())

    // language requested by the keyboard app through ACTION_DOWNLOAD_DICTIONARY, if any
    private var requestedLang: String? = null
    private var callingPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.dictionary_status)
        downloadButton = findViewById(R.id.download_button)
        downloadButton.setOnClickListener {
            AVAILABLE_DICTIONARIES.forEach {
                DictionaryDownloader.enqueueDownload(this, it)
            }
            refreshStatus()
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_DOWNLOAD_DICTIONARY) {
            requestedLang = intent.getStringExtra(EXTRA_LANG)
            callingPackage = intent.getStringExtra(EXTRA_CALLING_PACKAGE)
            val lang = requestedLang
            if (lang != null) {
                status.text = getString(R.string.downloading_dictionary, lang)
                DictionaryDownloader.enqueueDownload(this, lang) { success ->
                    mainHandler.post { onRequestedDownloadFinished(lang, success) }
                }
            }
        }
    }

    private fun onRequestedDownloadFinished(lang: String, success: Boolean) {
        refreshStatus()
        if (success) {
            // let the calling app (the keyboard) read the file through this content URI,
            // even if it does not hold the provider read permission
            val uri = Uri.parse(dictionaryFileUri(lang))
            callingPackage?.let {
                try {
                    grantUriPermission(it, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
            }
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_LANG, lang).putExtra(EXTRA_RESULT_URI, uri.toString())
            )
        } else {
            setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_LANG, lang).putExtra(EXTRA_ERROR, "download failed"))
        }
        // hand control back to the keyboard app
        finish()
    }

    private fun refreshStatus() {
        val lines = AVAILABLE_DICTIONARIES.map { lang ->
            val file = DictionaryDownloader.dictionaryFile(this, lang)
            when {
                DictionaryDownloader.isDownloaded(this, lang) ->
                    getString(R.string.dictionary_downloaded, lang, file.length() / 1024)

                DictionaryDownloader.isRunning(lang) ->
                    getString(R.string.downloading_dictionary, lang)

                else ->
                    getString(R.string.dictionary_missing, lang)
            }
        }
        if (requestedLang == null || AVAILABLE_DICTIONARIES.isEmpty()) {
            status.text = lines.joinToString("\n")
        }
    }
}
