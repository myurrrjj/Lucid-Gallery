package com.myurrrjj.chronicle.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.myurrrjj.chronicle.domain.model.AppSettings
import com.myurrrjj.chronicle.domain.model.FaceIndexingOptions
import com.myurrrjj.chronicle.domain.model.FaceIndexingSchedule
import com.myurrrjj.chronicle.domain.model.GalleryViewMode
import com.myurrrjj.chronicle.domain.repository.SettingsRepository
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

        val FACE_INDEXING_PAUSED = booleanPreferencesKey("face_indexing_paused")
        val FACE_INDEXING_SCHEDULE = stringPreferencesKey("face_indexing_schedule")
        val FACE_INDEXING_PAUSE_LOW_BATTERY =
            booleanPreferencesKey("face_indexing_pause_low_battery")
        val FACE_INDEXING_PAUSE_LOW_STORAGE =
            booleanPreferencesKey("face_indexing_pause_low_storage")
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


        val scheduleString = preferences[PreferencesKey.FACE_INDEXING_SCHEDULE]
            ?: FaceIndexingSchedule.MANUAL_ONLY.name
        val schedule = try {
            FaceIndexingSchedule.valueOf(scheduleString)
        } catch (e: Exception) {
            FaceIndexingSchedule.MANUAL_ONLY
        }

        AppSettings(
            autoPlayVideo = autoPlay,
            defaultGalleryViewMode = viewMode,
            faceIndexingPaused = preferences[PreferencesKey.FACE_INDEXING_PAUSED]?:false,
            faceIndexingOptions = FaceIndexingOptions(
                schedule = schedule,
                pauseOnLowBattery = preferences[PreferencesKey.FACE_INDEXING_PAUSE_LOW_BATTERY]?:true,
                pauseOnLowStorage = preferences[PreferencesKey.FACE_INDEXING_PAUSE_LOW_STORAGE]?:true
            )
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

    override suspend fun setFaceIndexingPaused(paused: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.FACE_INDEXING_PAUSED] = paused
        }
    }

    override suspend fun updateFaceIndexingOptions(options: FaceIndexingOptions) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.FACE_INDEXING_SCHEDULE] = options.schedule.name
            preferences[PreferencesKey.FACE_INDEXING_PAUSE_LOW_BATTERY] = options.pauseOnLowBattery
            preferences[PreferencesKey.FACE_INDEXING_PAUSE_LOW_STORAGE] = options.pauseOnLowStorage
        }
    }
}