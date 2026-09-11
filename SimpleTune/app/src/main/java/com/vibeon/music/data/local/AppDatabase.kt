package com.vibeon.music.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vibeon.music.data.local.dao.LibraryDao
import com.vibeon.music.data.local.entity.FavoriteEntity
import com.vibeon.music.data.local.entity.PlaylistEntity
import com.vibeon.music.data.local.entity.PlaylistSongEntity
import com.vibeon.music.data.local.entity.RecentEntity

@Database(
    entities = [
        FavoriteEntity::class,
        RecentEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "vibeon.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}