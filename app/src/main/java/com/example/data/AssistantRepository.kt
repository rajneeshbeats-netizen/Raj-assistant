package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for storing and retrieving Raj Assistant history, shortcuts, and settings.
 */
class AssistantRepository(private val db: RajDatabase) {

    val history: Flow<List<CommandHistoryEntity>> = db.historyDao().getAllHistory()
    val shortcuts: Flow<List<CustomShortcutEntity>> = db.shortcutDao().getAllShortcuts()
    val settings: Flow<AssistantSettingsEntity?> = db.settingsDao().getSettings()

    suspend fun logCommand(historyItem: CommandHistoryEntity) {
        db.historyDao().insertHistory(historyItem)
    }

    suspend fun clearAllHistory() {
        db.historyDao().clearHistory()
    }

    suspend fun saveShortcut(shortcut: CustomShortcutEntity) {
        db.shortcutDao().insertShortcut(shortcut)
    }

    suspend fun deleteShortcut(shortcut: CustomShortcutEntity) {
        db.shortcutDao().deleteShortcut(shortcut)
    }

    suspend fun updateSettings(newSettings: AssistantSettingsEntity) {
        db.settingsDao().saveSettings(newSettings)
    }
}
