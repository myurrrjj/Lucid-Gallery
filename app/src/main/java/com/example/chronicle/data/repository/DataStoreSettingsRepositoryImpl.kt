package com.example.chronicle.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.chronicle.domain.model.AppSettings
import com.example.chronicle.domain.model.GalleryViewMode
import com.example.chronicle.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DataStoreSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    private object PreferencesKey {
        val AUTO_PLAY_VIDEOS = booleanPreferencesKey("auto_play_videos")
        val DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
    }

    override val settings: Flow<AppSettings> = dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val autoPlay = preferences[PreferencesKey.AUTO_PLAY_VIDEOS] ?: true
            val viewModeString =
                preferences[PreferencesKey.DEFAULT_VIEW_MODE] ?: GalleryViewMode.DAY.name
            val viewMode = try {
                GalleryViewMode.valueOf(viewModeString)
            } catch (e: Exception) {
                GalleryViewMode.DAY
            }
        AppSettings(
            autoPlayVideo = autoPlay, defaultGalleryViewMode = viewMode
        )
        }

    override suspend fun updateAutoPlayVideos(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.AUTO_PLAY_VIDEOS] = enabled
        }
    }


    override suspend fun updateDefaultGalleryViewMode(viewMode: GalleryViewMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.DEFAULT_VIEW_MODE] = viewMode.name
        }
    }
}