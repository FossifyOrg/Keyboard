package org.fossify.dictionnary.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import org.fossify.dictionnary.helpers.AUTHORITY
import org.fossify.dictionnary.helpers.AVAILABLE_DICTIONARIES
import org.fossify.dictionnary.helpers.CODE_DICTIONARIES
import org.fossify.dictionnary.helpers.CODE_DICTIONARY
import org.fossify.dictionnary.helpers.CODE_FILE
import org.fossify.dictionnary.helpers.COLUMN_DOWNLOADED
import org.fossify.dictionnary.helpers.COLUMN_ID
import org.fossify.dictionnary.helpers.COLUMN_LANG
import org.fossify.dictionnary.helpers.COLUMN_NAME
import org.fossify.dictionnary.helpers.COLUMN_SIZE
import org.fossify.dictionnary.helpers.COLUMN_URI
import org.fossify.dictionnary.helpers.DictionaryDownloader
import org.fossify.dictionnary.helpers.EXTRA_LANG
import org.fossify.dictionnary.helpers.EXTRA_LANGUAGES
import org.fossify.dictionnary.helpers.EXTRA_STARTED
import org.fossify.dictionnary.helpers.METHOD_DOWNLOAD
import org.fossify.dictionnary.helpers.METHOD_LIST
import org.fossify.dictionnary.helpers.PATH_DICTIONARIES
import org.fossify.dictionnary.helpers.PATH_FILES
import org.fossify.dictionnary.helpers.dictionaryFileUri
import java.io.File
import java.io.FileNotFoundException

/**
 * Shares the downloaded dictionary files with the keyboard app through content URIs.
 *
 * - content://AUTHORITY/dictionaries            -> cursor with one row per dictionary
 * - content://AUTHORITY/dictionaries/<lang>     -> cursor with a single row for <lang>
 * - content://AUTHORITY/files/dictionaries/<lang>.json -> the file itself (openFile)
 *
 * Rows contain the columns _id, lang, name, uri, size and downloaded. Asking for a
 * dictionary that is not downloaded yet automatically enqueues its download.
 */
class DictionaryProvider : ContentProvider() {
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, PATH_DICTIONARIES, CODE_DICTIONARIES)
        addURI(AUTHORITY, "$PATH_DICTIONARIES/*", CODE_DICTIONARY)
        addURI(AUTHORITY, "$PATH_FILES/*", CODE_FILE)
        addURI(AUTHORITY, "$PATH_FILES/*/*", CODE_FILE)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val context = requireProviderContext()
        val cursor = MatrixCursor(
            arrayOf(COLUMN_ID, COLUMN_LANG, COLUMN_NAME, COLUMN_URI, COLUMN_SIZE, COLUMN_DOWNLOADED)
        )
        when (uriMatcher.match(uri)) {
            CODE_DICTIONARIES -> AVAILABLE_DICTIONARIES.forEachIndexed { index, lang ->
                cursor.addRow(dictionaryRow(context, index, lang))
            }

            CODE_DICTIONARY -> {
                val lang = uri.lastPathSegment.orEmpty()
                if (DictionaryDownloader.isValidLang(lang)) {
                    cursor.addRow(dictionaryRow(context, 0, lang))
                }
            }
        }
        return cursor
    }

    private fun dictionaryRow(context: Context, id: Int, lang: String): Array<Any?> {
        val file = DictionaryDownloader.dictionaryFile(context, lang)
        val downloaded = DictionaryDownloader.isDownloaded(context, lang)
        if (!downloaded) {
            // make sure asking for a dictionary also starts its download
            DictionaryDownloader.enqueueDownload(context, lang)
        }
        return arrayOf(
            id,
            lang,
            "$lang.json",
            dictionaryFileUri(lang),
            file.length(),
            if (downloaded) 1 else 0
        )
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (uriMatcher.match(uri) != CODE_FILE) {
            throw FileNotFoundException("unsupported uri: $uri")
        }
        val context = requireProviderContext()
        val relativePath = uri.pathSegments.drop(1).joinToString("/")
        val dictionariesRoot = File(context.filesDir, PATH_DICTIONARIES)
        val file = File(context.filesDir, relativePath)
        if (!file.canonicalPath.startsWith(dictionariesRoot.canonicalPath + File.separator)) {
            throw FileNotFoundException("forbidden path: $relativePath")
        }
        val lang = file.name.removeSuffix(".json")
        if (!DictionaryDownloader.isDownloaded(context, lang)) {
            DictionaryDownloader.enqueueDownload(context, lang)
            throw FileNotFoundException("dictionary '$lang' is not downloaded yet")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val context = context ?: return Bundle()
        val result = Bundle()
        when (method) {
            METHOD_DOWNLOAD -> {
                val lang = arg ?: extras?.getString(EXTRA_LANG).orEmpty()
                DictionaryDownloader.enqueueDownload(context, lang)
                result.putString(EXTRA_LANG, lang)
                result.putBoolean(EXTRA_STARTED, DictionaryDownloader.isValidLang(lang))
            }

            METHOD_LIST -> {
                result.putStringArray(EXTRA_LANGUAGES, AVAILABLE_DICTIONARIES.toTypedArray())
            }
        }
        return result
    }

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        CODE_DICTIONARIES -> "vnd.android.cursor.dir/vnd.$AUTHORITY.dictionary"
        CODE_DICTIONARY -> "vnd.android.cursor.item/vnd.$AUTHORITY.dictionary"
        else -> "application/json"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    private fun requireProviderContext(): Context = context ?: throw IllegalStateException("Provider not attached")
}
