package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pulsestream_prefs")

class AppPreferences(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val languageKey = stringPreferencesKey("language_code")

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "system"
    }

    val languageCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[languageKey] ?: "en"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[languageKey] = code
        }
    }
}
