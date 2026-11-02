package com.example.chronicle.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.chronicle.data.repository.DataStoreSettingsRepositoryImpl
import com.example.chronicle.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindingModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        dataStoreSettingsRepository: DataStoreSettingsRepositoryImpl
    ): SettingsRepository


}

@Module
@InstallIn(SingletonComponent::class)
object SettingProviderModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("app_settings") }
        )
    }
}