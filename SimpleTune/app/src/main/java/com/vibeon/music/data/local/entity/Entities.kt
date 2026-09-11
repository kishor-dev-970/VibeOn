package com.vibeon.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artists: String,
    val albumName: String?,
    val thumbnailUrl: String?,
    val duration: Long,
    val addedAt: Long,
)

@Entity(tableName = "recent")
data class RecentEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artists: String,
    val thumbnailUrl: String?,
    val duration: Long,
    val playedAt: Long,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "videoId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val position: Int,
    val videoId: String,
    val title: String,
    val artists: String,
    val thumbnailUrl: String?,
    val duration: Long,
)