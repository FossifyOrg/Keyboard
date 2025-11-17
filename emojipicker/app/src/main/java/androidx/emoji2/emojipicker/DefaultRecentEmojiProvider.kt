package com.rishabh.emojipicker

import android.content.Context
import android.content.Context.MODE_PRIVATE

/**
 * Provides recently shared emoji. This is the default recent emoji list provider. Clients could
 * specify the provider by their own.
 */
internal class DefaultRecentEmojiProvider(context: Context) : RecentEmojiProvider {

    companion object {
        private const val PREF_KEY_RECENT_EMOJI = "pref_key_recent_emoji"
        private const val RECENT_EMOJI_LIST_FILE_NAME = "androidx.emoji2.emojipicker.preferences"
        private const val SPLIT_CHAR = ","
    }

    private val sharedPreferences =
        context.getSharedPreferences(RECENT_EMOJI_LIST_FILE_NAME, MODE_PRIVATE)
    private val recentEmojiList: MutableList<String> =
        sharedPreferences.getString(PREF_KEY_RECENT_EMOJI, null)?.split(SPLIT_CHAR)?.toMutableList()
            ?: mutableListOf()

    override suspend fun getRecentEmojiList(): List<String> {
        return recentEmojiList
    }

    override fun recordSelection(emoji: String) {
        recentEmojiList.remove(emoji)
        recentEmojiList.add(0, emoji)
        saveToPreferences()
    }

    private fun saveToPreferences() {
        sharedPreferences
            .edit()
            .putString(PREF_KEY_RECENT_EMOJI, recentEmojiList.joinToString(SPLIT_CHAR))
            .commit()
    }
}
