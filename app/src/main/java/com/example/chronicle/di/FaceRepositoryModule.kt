package com.example.chronicle.di

import com.example.chronicle.data.repository.FaceRepositoryImpl
import com.example.chronicle.domain.repository.FaceRepository
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