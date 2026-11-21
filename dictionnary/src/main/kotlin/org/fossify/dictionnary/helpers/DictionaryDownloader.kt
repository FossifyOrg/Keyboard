package org.fossify.dictionnary.helpers

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.log10
import kotlin.math.roundToLong

/**
 * Downloads the dictionary files. This is the only place in the pair of apps where the
 * network is used: the Dictionnary app holds the INTERNET permission, the keyboard app
 * never touches the network and only reads the files through the content provider.
 *
 * The raw "word count" lists (see [DICTIONARY_BASE_URL]) are converted on the fly to the
 * JSON format the keyboard loads: [ [word, log10(frequency)], ... ], most frequent first.
 */
object DictionaryDownloader {
    // number of words kept per dictionary, same order of magnitude as the bundled en_US.json
    private const val MAX_DICTIONARY_WORDS = 25000

    private val executor = Executors.newCachedThreadPool()
    private val running = ConcurrentHashMap<String, Boolean>()
    private val listeners = ConcurrentHashMap<String, MutableList<(Boolean) -> Unit>>()

    fun dictionaryFile(context: Context, lang: String): File =
        File(context.filesDir, "$PATH_DICTIONARIES/$lang.json")

    fun isDownloaded(context: Context, lang: String): Boolean {
        val file = dictionaryFile(context, lang)
        return file.isFile && file.length() > 0
    }

    fun isRunning(lang: String): Boolean = running.containsKey(lang)

    fun isValidLang(lang: String): Boolean =
        lang.matches(Regex("[a-z]{2,3}(_[A-Za-z0-9]{2,8})*"))

    fun enqueueDownload(context: Context, lang: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (!isValidLang(lang) || dictionarySourceUrl(lang) == null) {
            onComplete?.invoke(false)
            return
        }
        if (isDownloaded(context, lang)) {
            onComplete?.invoke(true)
            return
        }
        if (onComplete != null) {
            listeners.getOrPut(lang) { mutableListOf() }.add(onComplete)
        }
        if (running.putIfAbsent(lang, true) != null) {
            return
        }
        val appContext = context.applicationContext
        executor.execute {
            val success = try {
                downloadBlocking(appContext, lang)
            } finally {
                running.remove(lang)
            }
            val callbacks = listeners.remove(lang) ?: emptyList()
            callbacks.forEach { it(success) }
        }
    }

    private fun downloadBlocking(context: Context, lang: String): Boolean {
        val sourceUrl = dictionarySourceUrl(lang) ?: return false
        val file = dictionaryFile(context, lang)
        file.parentFile?.mkdirs()
        val tmpFile = File(file.parentFile, file.name + ".tmp")
        val connection = URL(sourceUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.instanceFollowRedirects = true
        var success = false
        try {
            val entries = connection.inputStream.bufferedReader().use { reader ->
                readFrequencyEntries(reader)
            }
            if (entries.isNotEmpty()) {
                tmpFile.outputStream().use { output -> writeDictionaryJson(entries, output) }
                success = tmpFile.isFile && tmpFile.length() > 0
            }
            if (success) {
                if (file.exists()) {
                    file.delete()
                }
                success = tmpFile.renameTo(file)
            }
        } catch (_: Exception) {
            success = false
        } finally {
            if (!success) {
                tmpFile.delete()
            }
            connection.disconnect()
        }
        return success
    }

    /** reads "word count" lines from a raw frequency list, most frequent words first */
    private fun readFrequencyEntries(reader: BufferedReader): List<Pair<String, Long>> {
        val entries = ArrayList<Pair<String, Long>>(MAX_DICTIONARY_WORDS)
        while (entries.size < MAX_DICTIONARY_WORDS) {
            val line = reader.readLine() ?: break
            val separator = line.lastIndexOf(' ')
            if (separator <= 0) {
                continue
            }
            val count = line.substring(separator + 1).toLongOrNull() ?: continue
            if (count <= 0) {
                continue
            }
            entries.add(line.substring(0, separator) to count)
        }
        return entries
    }

    /** writes the entries as wordfreq-style JSON: [ [word, log10(frequency)], ... ] */
    private fun writeDictionaryJson(entries: List<Pair<String, Long>>, output: OutputStream) {
        val total = entries.sumOf { it.second }.toDouble()
        val array = JSONArray()
        for ((word, count) in entries) {
            val logFrequency = (log10(count.toDouble() / total) * 10000).roundToLong() / 10000.0
            array.put(JSONArray().put(word).put(logFrequency))
        }
        output.bufferedWriter().use { it.write(array.toString()) }
    }
}
