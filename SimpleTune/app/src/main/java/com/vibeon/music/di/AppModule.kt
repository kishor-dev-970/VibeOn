package com.vibeon.music.di

import android.content.Context
import androidx.room.Room
import com.vibeon.music.data.local.AppDatabase
import com.vibeon.music.data.local.dao.LibraryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "vibeon.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLibraryDao(database: AppDatabase): LibraryDao = database.libraryDao()
}