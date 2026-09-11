package com.vibeon.music.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibeon.music.data.local.entity.FavoriteEntity
import com.vibeon.music.data.local.entity.PlaylistEntity
import com.vibeon.music.data.local.entity.PlaylistSongEntity
import com.vibeon.music.data.local.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE videoId = :videoId")
    suspend fun favoriteById(videoId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE videoId = :videoId")
    suspend fun removeFavorite(videoId: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE videoId = :videoId")
    suspend fun isFavoriteCount(videoId: String): Int

    @Query("SELECT * FROM recent ORDER BY playedAt DESC")
    fun observeRecent(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecent(recent: RecentEntity)

    @Query("DELETE FROM recent")
    suspend fun clearRecent()

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun playlistById(id: Long): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY \"position\"")
    fun observePlaylistSongs(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPlaylistSong(song: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removePlaylistSong(playlistId: Long, videoId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("SELECT MAX(\"position\") FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun maxPlaylistPosition(playlistId: Long): Int?

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun playlistSongCount(playlistId: Long): Int
}