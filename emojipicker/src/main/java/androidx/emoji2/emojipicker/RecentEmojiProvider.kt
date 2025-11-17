

package com.rishabh.emojipicker

/** An interface to provide recent emoji list. */
interface RecentEmojiProvider {
    /**
     * Records an emoji into recent emoji list. This fun will be called when an emoji is selected.
     * Clients could specify the behavior to record recently used emojis.(e.g. click frequency).
     */
    fun recordSelection(emoji: String)

    /**
     * Returns a list of recent emojis. Default behavior: The most recently used emojis will be
     * displayed first. Clients could also specify the behavior such as displaying the emojis from
     * high click frequency to low click frequency.
     */
    suspend fun getRecentEmojiList(): List<String>
}
