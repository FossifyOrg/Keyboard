

package com.rishabh.emojipicker

/** A utility class to hold various constants used by the Emoji Picker library. */
internal object EmojiPickerConstants {

    // The default number of body columns.
    const val DEFAULT_BODY_COLUMNS = 9

    // The default number of rows of recent items held.
    const val DEFAULT_MAX_RECENT_ITEM_ROWS = 3

    // The max pool size of the Emoji ItemType in RecyclerViewPool.
    const val EMOJI_VIEW_POOL_SIZE = 100

    const val ADD_VIEW_EXCEPTION_MESSAGE = "Adding views to the EmojiPickerView is unsupported"

    const val REMOVE_VIEW_EXCEPTION_MESSAGE =
        "Removing views from the EmojiPickerView is unsupported"
}
