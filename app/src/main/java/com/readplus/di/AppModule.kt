package com.readplus.di

import android.content.Context
import androidx.room.Room
import com.readplus.data.local.ReadPlusDatabase
import com.readplus.data.local.dao.CategoryDao
import com.readplus.data.local.dao.ComicDao
import com.readplus.data.local.dao.VideoDao
import com.readplus.data.repository.*
import com.readplus.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): ReadPlusDatabase =
        Room.databaseBuilder(ctx, ReadPlusDatabase::class.java, "readplus.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideComicDao(db: ReadPlusDatabase): ComicDao = db.comicDao()
    @Provides fun provideVideoDao(db: ReadPlusDatabase): VideoDao = db.videoDao()
    @Provides fun provideCategoryDao(db: ReadPlusDatabase): CategoryDao = db.categoryDao()

    @Provides @Singleton
    fun provideComicRepository(impl: ComicRepositoryImpl): ComicRepository = impl

    @Provides @Singleton
    fun provideVideoRepository(impl: VideoRepositoryImpl): VideoRepository = impl

    @Provides @Singleton
    fun provideCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository = impl
}