package com.vibeon.music.data.local

import com.vibeon.music.data.local.dao.LibraryDao
import com.vibeon.music.data.local.entity.FavoriteEntity
import com.vibeon.music.data.local.entity.PlaylistEntity
import com.vibeon.music.data.local.entity.PlaylistSongEntity
import com.vibeon.music.data.local.entity.RecentEntity
import com.vibeon.music.domain.model.Artist
import com.vibeon.music.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val dao: LibraryDao,
) {
    val favorites: Flow<List<Song>> = dao.observeFavorites().map { list -> list.map { it.toSong() } }

    val recent: Flow<List<Song>> = dao.observeRecent().map { list -> list.map { it.toSong() } }

    val playlists: Flow<List<UserPlaylist>> = dao.observePlaylists().map { list ->
        list.map { pl ->
            UserPlaylist(id = pl.id, name = pl.name, createdAt = pl.createdAt)
        }
    }

    data class UserPlaylist(
        val id: Long,
        val name: String,
        val createdAt: Long,
    )

    suspend fun isFavorite(videoId: String): Boolean =
        dao.isFavoriteCount(videoId) > 0

    suspend fun toggleFavorite(song: Song): Boolean {
        val exists = isFavorite(song.videoId)
        if (exists) {
            dao.removeFavorite(song.videoId)
        } else {
            dao.addFavorite(song.toFavoriteEntity())
        }
        return !exists
    }

    suspend fun addToRecent(song: Song) {
        dao.addRecent(song.toRecentEntity())
    }

    suspend fun clearRecent() = dao.clearRecent()

    suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    suspend fun deletePlaylist(id: Long) = dao.deletePlaylist(id)

    suspend fun renamePlaylist(id: Long, name: String) = dao.renamePlaylist(id, name)

    suspend fun playlistSongCount(id: Long): Int = dao.playlistSongCount(id)

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        val position = (dao.maxPlaylistPosition(playlistId) ?: -1) + 1
        dao.addPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                position = position,
                videoId = song.videoId,
                title = song.title,
                artists = song.artists.joinToString(", ") { it.name },
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, videoId: String) =
        dao.removePlaylistSong(playlistId, videoId)

    suspend fun clearPlaylist(playlistId: Long) = dao.clearPlaylistSongs(playlistId)

    fun observePlaylistSongs(playlistId: Long): Flow<List<Song>> =
        dao.observePlaylistSongs(playlistId).map { list -> list.map { it.toSong() } }

    private fun FavoriteEntity.toSong(): Song = Song(
        videoId = videoId,
        title = title,
        artists = artists.splitArtists(),
        album = albumName?.let {
            com.vibeon.music.domain.model.Album(id = "", title = it)
        },
        duration = duration,
        thumbnailUrl = thumbnailUrl,
    )

    private fun RecentEntity.toSong(): Song = Song(
        videoId = videoId,
        title = title,
        artists = artists.splitArtists(),
        duration = duration,
        thumbnailUrl = thumbnailUrl,
    )

    private fun PlaylistSongEntity.toSong(): Song = Song(
        videoId = videoId,
        title = title,
        artists = artists.splitArtists(),
        duration = duration,
        thumbnailUrl = thumbnailUrl,
    )

    private fun String.splitArtists(): List<Artist> =
        split(", ").filter { it.isNotBlank() }.map { Artist(name = it) }

    private fun Song.toFavoriteEntity(): FavoriteEntity = FavoriteEntity(
        videoId = videoId,
        title = title,
        artists = artists.joinToString(", ") { it.name },
        albumName = album?.title,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        addedAt = System.currentTimeMillis(),
    )

    private fun Song.toRecentEntity(): RecentEntity = RecentEntity(
        videoId = videoId,
        title = title,
        artists = artists.joinToString(", ") { it.name },
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        playedAt = System.currentTimeMillis(),
    )
}