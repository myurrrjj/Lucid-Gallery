package com.example.chronicle.di

import com.example.chronicle.core.ml.MlKitFaceDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun provideMlKitFaceDetector(): MlKitFaceDetector {
        return MlKitFaceDetector()
    }
}