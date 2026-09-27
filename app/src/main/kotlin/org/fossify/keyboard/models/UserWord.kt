package org.fossify.keyboard.models

import androidx.room.ColumnInfo
import androidx.room.Entity

/** A word the user taught the keyboard, how often it was used and when it was last used, in epoch milliseconds. */
@Entity(tableName = "user_words", primaryKeys = ["word", "locale"])
data class UserWord(
    @ColumnInfo(name = "word") val word: String,
    @ColumnInfo(name = "locale") val locale: String,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "last_used") val lastUsed: Long
)
