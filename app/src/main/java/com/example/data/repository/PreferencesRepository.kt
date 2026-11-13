package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.findItDataStore: DataStore<Preferences> by preferencesDataStore(name = "findit_campus_prefs")

class PreferencesRepository(private val context: Context) {

    val savedPhoneNumber: Flow<String> = context.findItDataStore.data.map { prefs ->
        prefs[KEY_SAVED_PHONE] ?: ""
    }

    val systemNotificationsEnabled: Flow<Boolean> = context.findItDataStore.data.map { prefs ->
        prefs[KEY_SYSTEM_NOTIFICATIONS] ?: true
    }

    suspend fun savePhoneNumber(phone: String) {
        context.findItDataStore.edit { prefs ->
            prefs[KEY_SAVED_PHONE] = phone.trim()
        }
    }

    suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
        context.findItDataStore.edit { prefs ->
            prefs[KEY_SYSTEM_NOTIFICATIONS] = enabled
        }
    }

    private companion object {
        val KEY_SAVED_PHONE = stringPreferencesKey("saved_phone_number")
        val KEY_SYSTEM_NOTIFICATIONS = booleanPreferencesKey("system_notifications_enabled")
    }
}
