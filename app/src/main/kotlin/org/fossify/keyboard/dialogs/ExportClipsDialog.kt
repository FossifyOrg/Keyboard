package org.fossify.keyboard.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.dialogs.FilePickerDialog
import org.fossify.commons.extensions.*
import org.fossify.keyboard.R
import org.fossify.keyboard.databinding.DialogExportClipsBinding
import org.fossify.keyboard.extensions.config

class ExportClipsDialog(
    val activity: BaseSimpleActivity, path: String, val hidePath: Boolean, callback: (path: String, filename: String) -> Unit
) {
    init {
        var folder = if (path.isNotEmpty() && activity.getDoesFilePathExist(path)) {
            path
        } else {
            activity.internalStoragePath
        }

        val binding = DialogExportClipsBinding.inflate(activity.layoutInflater).apply {
            exportClipsFilename.setText("${activity.getString(R.string.app_launcher_name)}_${activity.getCurrentFormattedDateTime()}")

            if (hidePath) {
                exportClipsPathLabel.beGone()
                exportClipsPath.beGone()
            } else {
                exportClipsPath.text = activity.humanizePath(folder)
                exportClipsPath.setOnClickListener {
                    FilePickerDialog(activity, folder, false, showFAB = true) {
                        exportClipsPath.text = activity.humanizePath(it)
                        folder = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_clipboard_items) { alertDialog ->
                    alertDialog.showKeyboard(binding.exportClipsFilename)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.exportClipsFilename.value
                        if (filename.isEmpty()) {
                            activity.toast(R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val newPath = "${folder.trimEnd('/')}/$filename"
                        if (!newPath.getFilenameFromPath().isAValidFilename()) {
                            activity.toast(R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        activity.config.lastExportedClipsFolder = folder
                        if (!hidePath && activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newPath.getFilenameFromPath())
                            ConfirmationDialog(activity, title) {
                                callback(newPath, filename)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newPath, filename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
