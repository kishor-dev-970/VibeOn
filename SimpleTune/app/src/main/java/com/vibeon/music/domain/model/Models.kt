package com.vibeon.music.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Image(
    val url: String,
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class Artist(
    val id: String = "",
    val name: String,
    val thumbnailUrl: String? = null,
)

@Serializable
data class Album(
    val id: String = "",
    val title: String,
    val artists: List<Artist> = emptyList(),
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    val songCount: Int? = null,
)

@Serializable
data class Song(
    val videoId: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null,
    val duration: Long = 0,
    val thumbnailUrl: String? = null,
)

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val author: String = "",
    val thumbnailUrl: String? = null,
    val songCount: Int? = null,
    val description: String = "",
)

enum class ItemType { SONG, ALBUM, PLAYLIST, ARTIST, VIDEO, RADIO, UNKNOWN }

@Serializable
data class BrowseItem(
    val type: ItemType,
    val title: String,
    val subtitle: String = "",
    val videoId: String? = null,
    val browseId: String? = null,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
)

@Serializable
data class HomeChip(
    val label: String,
    val browseId: String,
    val params: String? = null,
)

@Serializable
enum class HomeSectionLayout { CAROUSEL, GRID, LIST }

@Serializable
data class HomeSection(
    val title: String,
    val items: List<BrowseItem>,
    val moreBrowseId: String? = null,
    val layout: HomeSectionLayout = HomeSectionLayout.CAROUSEL,
)

data class StreamUrl(
    val url: String,
    val mimeType: String = "",
    val bitrate: Int = 0,
    val codec: String = "",
    val durationMs: Long = 0,
)

data class PlayerResponse(
    val song: Song? = null,
    val streams: List<StreamUrl> = emptyList(),
    val playabilityStatus: String = "",
    val playabilityError: String? = null,
)

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val videos: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && videos.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}