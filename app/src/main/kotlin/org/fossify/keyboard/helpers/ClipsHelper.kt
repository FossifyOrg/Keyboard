package org.fossify.keyboard.helpers

import android.content.Context
import org.fossify.keyboard.extensions.clipsDB
import org.fossify.keyboard.models.Clip

class ClipsHelper(val context: Context) {

    // make sure clips have unique values
    fun insertClip(clip: Clip): Long {
        return if (context.clipsDB.getClipWithValue(clip.value) == null) {
            context.clipsDB.insertOrUpdate(clip)
        } else {
            -1
        }
    }
}
