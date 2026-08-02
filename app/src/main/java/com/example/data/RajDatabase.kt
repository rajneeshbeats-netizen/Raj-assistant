package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CommandHistoryEntity::class, CustomShortcutEntity::class, AssistantSettingsEntity::class],
    version = 3,
    exportSchema = false
)
abstract class RajDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: RajDatabase? = null

        fun getInstance(context: Context): RajDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RajDatabase::class.java,
                    "raj_assistant_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
