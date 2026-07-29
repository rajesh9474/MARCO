package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, ReminderEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MarcoDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MarcoDatabase? = null

        fun getDatabase(context: Context): MarcoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarcoDatabase::class.java,
                    "marco_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
