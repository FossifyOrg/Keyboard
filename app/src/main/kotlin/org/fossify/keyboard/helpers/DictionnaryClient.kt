package org.fossify.keyboard.helpers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.keyboard.R
import org.fossify.keyboard.extensions.safeStorageContext
import java.io.File

/**
 * Client for the external Dictionnary app (org.fossify.dictionnary).
 *
 * The keyboard app itself has no internet permission: the Dictionnary app downloads the
 * dictionary files and serves them through a content provider. Here we can:
 * - ask that provider for the content URI of a file, open the URI and keep a local copy
 *   of the downloaded file in our own files dir (see [copyDictionaryToLocal]),
 * - or explicitly launch the Dictionnary app with [DICTIONNARY_ACTION_DOWNLOAD] so it
 *   downloads a language and hands the result (and a read grant on the file URI) back
 *   to us via startActivityForResult (see [launchDictionaryDownload] / [handleDownloadResult]).
 */
object DictionnaryClient {
    /** request code used when picking the Dictionnary APK file with the system picker */
    const val REQUEST_PICK_DICTIONNARY_APK = 1002

    private const val PATH_DICTIONARIES = "dictionaries"
    private const val PATH_FILES = "files"
    private const val METHOD_DOWNLOAD = "download"
    private const val COLUMN_DOWNLOADED = "downloaded"
    private const val POLL_INTERVAL_MS = 500L
    private const val MAX_POLLS = 60

    fun dictionaryUri(lang: String): Uri =
        Uri.parse("content://$DICTIONNARY_AUTHORITY/$PATH_DICTIONARIES/$lang")

    fun dictionaryFileUri(lang: String): Uri =
        Uri.parse("content://$DICTIONNARY_AUTHORITY/$PATH_FILES/$PATH_DICTIONARIES/$lang.json")

    /** local copy of the dictionary inside the keyboard app's own storage */
    fun localDictionaryFile(context: Context, lang: String): File =
        File(context.safeStorageContext.filesDir, "$PATH_DICTIONARIES/$lang.json")

    fun isLocalDictionaryAvailable(context: Context, lang: String): Boolean {
        val file = localDictionaryFile(context, lang)
        return file.isFile && file.length() > 0
    }

    /** package name of the installed Dictionnary app (release or debug), null if not installed */
    fun dictionnaryPackageName(context: Context): String? {
        val packageManager = context.packageManager
        return listOf(DICTIONNARY_PACKAGE, "$DICTIONNARY_PACKAGE.debug").firstOrNull { packageName ->
            try {
                packageManager.getLaunchIntentForPackage(packageName) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    fun isDictionnaryInstalled(context: Context): Boolean = dictionnaryPackageName(context) != null

    /** asks the Dictionnary app's provider to (re)download the given language */
    fun requestDownload(context: Context, lang: String) {
        try {
            context.contentResolver.call(DICTIONNARY_AUTHORITY, METHOD_DOWNLOAD, lang, null)
        } catch (_: Exception) {
        }
    }

    /** whether the Dictionnary app already has the file, as reported by its provider */
    fun isDownloadedRemotely(context: Context, lang: String): Boolean = try {
        context.contentResolver.query(dictionaryUri(lang), null, null, null, null)?.use { cursor ->
            cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOADED)) == 1
        } ?: false
    } catch (_: Exception) {
        false
    }

    /**
     * Makes sure a local copy of the dictionary exists in the keyboard app. If there is none,
     * the Dictionnary app is asked to download it, we wait for the download to finish, then the
     * file exposed at its content URI is opened and copied into our own files dir.
     *
     * Blocking, must be called from a background thread. Returns the local file or null.
     */
    fun copyDictionaryToLocal(context: Context, lang: String): File? {
        val localFile = localDictionaryFile(context, lang)
        if (localFile.isFile && localFile.length() > 0) {
            return localFile
        }

        if (!isDictionnaryInstalled(context)) {
            return null
        }

        requestDownload(context, lang)
        var downloaded = isDownloadedRemotely(context, lang)
        var polls = 0
        while (!downloaded && polls < MAX_POLLS) {
            Thread.sleep(POLL_INTERVAL_MS)
            downloaded = isDownloadedRemotely(context, lang)
            polls++
        }
        if (!downloaded) {
            return null
        }

        return copyFromUri(context, dictionaryFileUri(lang), lang)
    }

    /** asynchronous variant of [copyDictionaryToLocal] */
    fun ensureLocalDictionary(context: Context, lang: String, onReady: (File?) -> Unit) {
        ensureBackgroundThread {
            onReady(copyDictionaryToLocal(context, lang))
        }
    }

    /** copies the file exposed at the given content URI into our local dictionaries dir */
    fun copyFromUri(context: Context, uri: Uri, lang: String): File? {
        val localFile = localDictionaryFile(context, lang)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                localFile.parentFile?.mkdirs()
                val tmpFile = File(localFile.parentFile, localFile.name + ".tmp")
                tmpFile.outputStream().use { output -> input.copyTo(output) }
                if (tmpFile.isFile && tmpFile.length() > 0) {
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    tmpFile.renameTo(localFile)
                    localFile
                } else {
                    tmpFile.delete()
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** intent launching the Dictionnary app to download [lang] and report back via activity result */
    fun downloadIntent(context: Context, lang: String): Intent =
        Intent(DICTIONNARY_ACTION_DOWNLOAD).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(DICTIONNARY_EXTRA_LANG, lang)
            putExtra(DICTIONNARY_EXTRA_CALLING_PACKAGE, context.packageName)
        }

    /**
     * Launches the Dictionnary app so it downloads the dictionary for [lang]. The result comes
     * back in onActivityResult: RESULT_OK with [DICTIONNARY_EXTRA_RESULT_URI] pointing to the
     * downloaded file (the Dictionnary app grants us read access to that URI).
     */
    fun launchDictionaryDownload(activity: Activity, lang: String, requestCode: Int) {
        try {
            activity.startActivityForResult(downloadIntent(activity, lang), requestCode)
        } catch (_: Exception) {
            proposeInstallDictionnary(activity, lang)
        }
    }

    /** same as [proposeInstallDictionnary] but without the lang-specific message context */
    private fun proposeInstallDictionnary(activity: Activity, lang: String) =
        proposeInstallDictionnary(activity, lang, REQUEST_PICK_DICTIONNARY_APK)

    /**
     * Handles the activity result of [launchDictionaryDownload]: copies the file the Dictionnary
     * app exposed (and granted us) into our local storage. Returns the local file or null.
     */
    fun handleDownloadResult(context: Context, resultCode: Int, data: Intent?): File? {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return null
        }
        val lang = data.getStringExtra(DICTIONNARY_EXTRA_LANG) ?: return null
        val uriString = data.getStringExtra(DICTIONNARY_EXTRA_RESULT_URI)
        val uri = uriString?.let { Uri.parse(it) } ?: dictionaryFileUri(lang)
        return copyFromUri(context, uri, lang)
    }

    /** opens the launcher activity of the installed Dictionnary app, if any */
    fun openDictionnaryApp(activity: Activity): Boolean {
        val packageName = dictionnaryPackageName(activity) ?: return false
        val launchIntent = activity.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            activity.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** lets the user pick the Dictionnary APK file with the system file picker */
    fun pickDictionnaryApk(activity: Activity, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.android.package-archive"
        }
        try {
            activity.startActivityForResult(intent, requestCode)
        } catch (_: Exception) {
        }
    }

    /** hands the picked APK content URI to the system package installer */
    fun installApkFromUri(activity: Activity, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Shows a dialog proposing to install the Dictionnary app (it is not installed yet):
     * either by picking its APK file (system file picker → package installer, works without
     * any store), or by opening its Play Store page.
     */
    fun proposeInstallDictionnary(activity: Activity, lang: String, pickRequestCode: Int) {
        activity.getAlertDialogBuilder()
            .setTitle(R.string.install_dictionnary_title)
            .setMessage(activity.getString(R.string.install_dictionnary_message, lang))
            .setPositiveButton(R.string.install_dictionnary_pick_apk) { _, _ ->
                pickDictionnaryApk(activity, pickRequestCode)
            }
            .setNeutralButton(R.string.open_play_store) { _, _ ->
                openDictionnaryStorePage(activity)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openDictionnaryStorePage(activity: Activity) {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$DICTIONNARY_PACKAGE"))
        try {
            activity.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$DICTIONNARY_PACKAGE")
                )
            )
        }
    }
}
