package org.fossify.keyboard.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.fossify.keyboard.interfaces.ClipsDao
import org.fossify.keyboard.models.Clip

@Database(entities = [Clip::class], version = 1)
abstract class ClipsDatabase : RoomDatabase() {

    abstract fun ClipsDao(): ClipsDao

    companion object {
        private var db: ClipsDatabase? = null

        fun getInstance(context: Context): ClipsDatabase {
            if (db == null) {
                synchronized(ClipsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context, ClipsDatabase::class.java, "clips.db").build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }
    }
}
