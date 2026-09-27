package org.fossify.keyboard.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.fossify.keyboard.interfaces.UserWordsDao
import org.fossify.keyboard.models.UserWord

/** The words the user taught the keyboard, kept apart from the clips so neither database needs a migration. */
@Database(entities = [UserWord::class], version = 1)
abstract class UserWordsDatabase : RoomDatabase() {

    abstract fun UserWordsDao(): UserWordsDao

    companion object {
        private var db: UserWordsDatabase? = null

        fun getInstance(context: Context): UserWordsDatabase {
            if (db == null) {
                synchronized(UserWordsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context, UserWordsDatabase::class.java, "user_words.db").build()
                    }
                }
            }
            return db!!
        }
    }
}
