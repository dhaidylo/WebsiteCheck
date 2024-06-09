package com.example.websitecheck

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first


val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val MIN_AREA = intPreferencesKey("min_area")
    val MAX_AREA = intPreferencesKey("max_area")
    val MAX_PRICE = intPreferencesKey("max_price")
    val CHECK_INTERVAL = intPreferencesKey("check_interval")
}

data class Settings(
    var minArea: Int = 30,
    var maxArea: Int = 55,
    var maxPrice: Int = 600,
    var checkInterval: Int = 10
)

suspend fun setSettings(context: Context, settings: Settings) {
    context.settings.edit {
        it[SettingsKeys.MIN_AREA] = settings.minArea
        it[SettingsKeys.MAX_AREA] = settings.maxArea
        it[SettingsKeys.MAX_PRICE] = settings.maxPrice
        it[SettingsKeys.CHECK_INTERVAL] = settings.checkInterval
    }
}

suspend fun readSettings(context: Context): Settings {
    val settings = context.settings.data.first()
    return Settings(
        settings[SettingsKeys.MIN_AREA] ?: Settings().minArea,
        settings[SettingsKeys.MAX_AREA] ?: Settings().maxArea,
        settings[SettingsKeys.MAX_PRICE] ?: Settings().maxPrice,
        settings[SettingsKeys.CHECK_INTERVAL] ?: Settings().checkInterval
    )
}