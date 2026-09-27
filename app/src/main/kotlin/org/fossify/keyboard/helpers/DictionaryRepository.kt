package org.fossify.keyboard.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.keyboard.suggestions.ArrayTrie
import org.fossify.keyboard.suggestions.DictionaryLoader
import java.io.IOException

/** Loads word suggestion dictionaries from the assets on a background thread, once per process and locale. */
class DictionaryRepository(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val tries = HashMap<String, ArrayTrie>()
    private val loading = HashSet<String>()

    /** Calls [onLoaded] on the main thread with the dictionary of [locale], unless it's missing from the assets. */
    fun load(locale: String, onLoaded: (ArrayTrie) -> Unit) {
        val trie = tries[locale]
        if (trie != null) {
            onLoaded(trie)
            return
        }

        if (!loading.add(locale)) return
        ensureBackgroundThread {
            val loaded = read(locale)
            handler.post {
                loading.remove(locale)
                if (loaded != null) {
                    tries[locale] = loaded
                    onLoaded(loaded)
                }
            }
        }
    }

    private fun read(locale: String): ArrayTrie? {
        return try {
            context.assets.open("$DICTIONARIES_FOLDER/$locale.tsv").use { DictionaryLoader.load(it) }
        } catch (ignored: IOException) {
            null
        }
    }
}
