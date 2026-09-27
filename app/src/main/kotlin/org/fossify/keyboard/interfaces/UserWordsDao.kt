package org.fossify.keyboard.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.keyboard.models.UserWord

@Dao
interface UserWordsDao {
    @Query("SELECT * FROM user_words WHERE locale = :locale ORDER BY last_used")
    fun getWords(locale: String): List<UserWord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(word: UserWord)

    @Query("DELETE FROM user_words WHERE word = :word AND locale = :locale")
    fun delete(word: String, locale: String)

    /** Deletes the least recently used words of [locale] beyond the [limit] most recent ones. */
    @Query(
        "DELETE FROM user_words WHERE locale = :locale AND word NOT IN " +
            "(SELECT word FROM user_words WHERE locale = :locale ORDER BY last_used DESC LIMIT :limit)"
    )
    fun trim(locale: String, limit: Int)

    @Query("DELETE FROM user_words")
    fun deleteAll()
}
