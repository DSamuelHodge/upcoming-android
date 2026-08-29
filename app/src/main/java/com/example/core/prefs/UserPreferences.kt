package com.example.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

/** Device-local notification preferences (per-device by nature — push/alarms
 *  fire on this handset only, so these never sync to the server). */
data class NotificationPrefs(
    val pushAlertsEnabled: Boolean = true,
    val soundVibrateEnabled: Boolean = true,
    val tenMinReminderEnabled: Boolean = true
)

class UserPreferences(private val context: Context) {
    private val pushKey = booleanPreferencesKey("push_alerts_enabled")
    private val soundKey = booleanPreferencesKey("sound_vibrate_enabled")
    private val tenMinKey = booleanPreferencesKey("ten_min_reminder_enabled")

    val notificationPrefs: Flow<NotificationPrefs> = context.userPrefsDataStore.data.map { p ->
        NotificationPrefs(
            pushAlertsEnabled = p[pushKey] ?: true,
            soundVibrateEnabled = p[soundKey] ?: true,
            tenMinReminderEnabled = p[tenMinKey] ?: true
        )
    }

    suspend fun setPushAlertsEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[pushKey] = enabled }
    }

    suspend fun setSoundVibrateEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[soundKey] = enabled }
    }

    suspend fun setTenMinReminderEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[tenMinKey] = enabled }
    }
}
