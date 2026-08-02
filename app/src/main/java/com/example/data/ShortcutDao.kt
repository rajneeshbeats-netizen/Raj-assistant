package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM custom_shortcuts ORDER BY id DESC")
    fun getAllShortcuts(): Flow<List<CustomShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: CustomShortcutEntity)

    @Delete
    suspend fun deleteShortcut(shortcut: CustomShortcutEntity)
}
