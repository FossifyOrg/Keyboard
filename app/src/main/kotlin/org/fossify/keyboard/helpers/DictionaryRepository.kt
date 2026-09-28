package org.fossify.keyboard.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.keyboard.suggestions.ArrayTrie
import org.fossify.keyboard.suggestions.DictionaryLoader

/**
 * Loads word suggestion dictionaries from the assets on a background thread, once per process and locale. Only the
 * most recently used ones are kept, as someone switching languages mostly switches between two.
 */
class DictionaryRepository(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val tries = object : LinkedHashMap<String, ArrayTrie>(MAX_LOADED, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ArrayTrie>) = size > MAX_LOADED
    }
    private val loading = HashSet<String>()
    private val unavailable = HashSet<String>()

    /** Calls [onLoaded] on the main thread with the dictionary of [locale], unless it's missing or broken. */
    fun load(locale: String, onLoaded: (ArrayTrie) -> Unit) {
        val trie = tries[locale]
        if (trie != null) {
            onLoaded(trie)
            return
        }

        if (locale in unavailable || !loading.add(locale)) return
        ensureBackgroundThread {
            val loaded = read(locale)
            handler.post {
                loading.remove(locale)
                if (loaded != null) {
                    tries[locale] = loaded
                    onLoaded(loaded)
                } else {
                    unavailable += locale
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun read(locale: String): ArrayTrie? {
        return try {
            context.assets.open("$DICTIONARIES_FOLDER/$locale.dict").use { DictionaryLoader.load(it) }
        } catch (e: Exception) {
            // A missing or malformed dictionary means no suggestions rather than a crashed keyboard
            Log.w(TAG, "Dictionary $locale is unavailable", e)
            null
        }
    }

    private companion object {
        const val TAG = "DictionaryRepository"
        const val MAX_LOADED = 2
        const val LOAD_FACTOR = 0.75f
    }
}
