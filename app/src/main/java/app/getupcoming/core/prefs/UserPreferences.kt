package app.getupcoming.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

/** Reminder settings. [reminderOffsets] are minutes-before-start lead times,
 *  sorted ascending (soonest first) — mirrors users.metadata.prefs on the
 *  server, but DataStore is the device-local source the alarm engine reads. */
data class NotificationPrefs(
    val pushAlertsEnabled: Boolean = true,
    val soundVibrateEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val reminderOffsets: List<Int> = DEFAULT_REMINDER_OFFSETS
)

val DEFAULT_REMINDER_OFFSETS = listOf(10)

/** Presets offered by the reminder editor (minutes before start). */
val REMINDER_PRESETS = listOf(5, 10, 15, 30, 60, 1440, 2880)

class UserPreferences(private val context: Context) {
    private val pushKey = booleanPreferencesKey("push_alerts_enabled")
    private val soundKey = booleanPreferencesKey("sound_vibrate_enabled")
    private val remindersKey = booleanPreferencesKey("reminders_enabled")
    private val offsetsKey = stringPreferencesKey("reminder_offsets_minutes")
    // Legacy key from the developer-mode build; migrated in the mapping below.
    private val legacyTenMinKey = booleanPreferencesKey("ten_min_reminder_enabled")

    val notificationPrefs: Flow<NotificationPrefs> = context.userPrefsDataStore.data.map { p ->
        val legacyTenMin = p[legacyTenMinKey]
        NotificationPrefs(
            pushAlertsEnabled = p[pushKey] ?: true,
            soundVibrateEnabled = p[soundKey] ?: true,
            remindersEnabled = p[remindersKey] ?: legacyTenMin ?: true,
            reminderOffsets = p[offsetsKey]?.parseOffsets() ?: DEFAULT_REMINDER_OFFSETS
        )
    }

    suspend fun setPushAlertsEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[pushKey] = enabled }
    }

    suspend fun setSoundVibrateEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[soundKey] = enabled }
    }

    suspend fun setReminderSettings(enabled: Boolean, offsets: List<Int>) {
        context.userPrefsDataStore.edit {
            it[remindersKey] = enabled
            it[offsetsKey] = offsets.joinToString(",")
        }
    }
}

/** Parses the stored "10,60,1440" form; drops garbage, dedupes, sorts. */
internal fun String.parseOffsets(): List<Int> =
    split(',').mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..10080 }
        .toSortedSet()
        .toList()
        .ifEmpty { DEFAULT_REMINDER_OFFSETS }
