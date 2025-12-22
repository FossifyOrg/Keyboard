package org.fossify.keyboard.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.util.Collections
import kotlin.math.abs
import kotlin.math.min

private const val DEBOUNCE_DELAY_MS = 100L
private const val MAX_SUGGESTIONS = 3
private const val MAX_LENGTH_DIFF = 2
private const val MAX_EDIT_DISTANCE = 2
private const val MIN_WORD_LENGTH = 2
private const val CACHE_SIZE = 20

class SpellChecker(context: Context) {
    @Volatile
    private var dictionary: HashMap<String, Int>? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null
    @Suppress("MagicNumber")
    private val cache: MutableMap<String, List<String>> = Collections.synchronizedMap(
        object : LinkedHashMap<String, List<String>>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>) = size > CACHE_SIZE
        }
    )

    var onSuggestionsReady: ((List<String>) -> Unit)? = null

    init {
        ensureBackgroundThread {
            dictionary = loadDictionary(context)
        }
    }

    fun checkWord(word: String) {
        pendingRunnable?.let { mainHandler.removeCallbacks(it) }

        pendingRunnable = Runnable {
            ensureBackgroundThread {
                val suggestions = findSuggestions(word)
                mainHandler.post {
                    onSuggestionsReady?.invoke(suggestions)
                }
            }
        }

        mainHandler.postDelayed(pendingRunnable!!, DEBOUNCE_DELAY_MS)
    }

    fun clear() {
        pendingRunnable?.let { mainHandler.removeCallbacks(it) }
        mainHandler.post {
            onSuggestionsReady?.invoke(emptyList())
        }
    }

    fun isValidWord(word: String): Boolean {
        return dictionary?.containsKey(word.lowercase()) ?: true
    }

    fun destroy() {
        pendingRunnable?.let { mainHandler.removeCallbacks(it) }
        onSuggestionsReady = null
    }

    private fun loadDictionary(context: Context): HashMap<String, Int> {
        val map = HashMap<String, Int>()
        try {
            val inputStream = context.assets.open("dictionaries/en_US.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val entry = jsonArray.getJSONArray(i)
                val word = entry.getString(0)
                val rank = jsonArray.length() - i
                map[word] = rank
            }
        } catch (_: IOException) {
        } catch (_: JSONException) {
        }
        return map
    }

    private fun findSuggestions(input: String): List<String> {
        val dict = dictionary ?: return emptyList()
        if (input.length < MIN_WORD_LENGTH) return emptyList()

        val inputLower = input.lowercase()

        cache[inputLower]?.let { return it }

        val prefixMatches = dict.keys
            .filter { word -> word.startsWith(inputLower) && word != inputLower }
            .sortedByDescending { dict[it] ?: 0 }
            .take(MAX_SUGGESTIONS)

        if (prefixMatches.isNotEmpty()) {
            cache[inputLower] = prefixMatches
            return prefixMatches
        }

        val firstChar = inputLower[0]
        val inputLen = inputLower.length

        val levenshteinMatches = dict.keys
            .filter { word ->
                word[0] == firstChar && abs(word.length - inputLen) <= MAX_LENGTH_DIFF
            }
            .map { word ->
                word to levenshteinDistance(inputLower, word)
            }
            .filter { it.second <= MAX_EDIT_DISTANCE }
            .sortedWith(compareBy({ it.second }, { -(dict[it.first] ?: 0) }))
            .take(MAX_SUGGESTIONS)
            .map { it.first }

        cache[inputLower] = levenshteinMatches
        return levenshteinMatches
    }

    private fun levenshteinDistance(a: String, b: String, maxDistance: Int = MAX_EDIT_DISTANCE): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            var rowMin = Int.MAX_VALUE
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
                rowMin = min(rowMin, dp[i][j])
            }
            if (rowMin > maxDistance) return maxDistance + 1
        }

        return dp[m][n]
    }
}
