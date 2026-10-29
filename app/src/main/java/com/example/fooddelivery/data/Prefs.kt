package com.example.fooddelivery.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class Prefs(private val context: Context) {
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val notificationsKey = booleanPreferencesKey("notifications")
    private val autoRefreshKey = booleanPreferencesKey("auto_refresh")

    val onboarded = context.dataStore.data.map { it[onboardedKey] ?: false }
    val notifications = context.dataStore.data.map { it[notificationsKey] ?: true }
    val autoRefresh = context.dataStore.data.map { it[autoRefreshKey] ?: false }

    suspend fun setOnboarded(v: Boolean) = context.dataStore.edit { it[onboardedKey] = v }
    suspend fun setNotifications(v: Boolean) = context.dataStore.edit { it[notificationsKey] = v }
    suspend fun setAutoRefresh(v: Boolean) = context.dataStore.edit { it[autoRefreshKey] = v }
}
