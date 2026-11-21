package org.fossify.dictionnary

import android.app.Application
import org.fossify.dictionnary.helpers.AVAILABLE_DICTIONARIES
import org.fossify.dictionnary.helpers.DictionaryDownloader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // pre-download every known dictionary so the keyboard can use them right away
        AVAILABLE_DICTIONARIES.forEach {
            DictionaryDownloader.enqueueDownload(this, it)
        }
    }
}
