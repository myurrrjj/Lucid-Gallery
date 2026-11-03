package com.myurrrjj.chronicle.di

import com.myurrrjj.chronicle.data.repository.FaceRepositoryImpl
import com.myurrrjj.chronicle.domain.repository.FaceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class FaceRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFaceRepository(
        faceRepositoryImpl: FaceRepositoryImpl
    ): FaceRepository
}