package com.vibeon.music.data.music

import com.vibeon.music.data.innertube.AlbumResponse
import com.vibeon.music.data.innertube.ArtistResponse
import com.vibeon.music.data.innertube.ExploreData
import com.vibeon.music.data.innertube.HomeSectionResponse
import com.vibeon.music.data.innertube.InnerTubeClient
import com.vibeon.music.data.innertube.InnerTubeParser
import com.vibeon.music.data.innertube.PlaylistResponse
import com.vibeon.music.data.innertube.PoTokenMinter
import com.vibeon.music.data.settings.AudioQuality
import com.vibeon.music.domain.model.Album
import com.vibeon.music.domain.model.Artist
import com.vibeon.music.domain.model.BrowseItem
import com.vibeon.music.domain.model.HomeSection
import com.vibeon.music.domain.model.HomeSectionKind
import com.vibeon.music.domain.model.Playlist
import com.vibeon.music.domain.model.PlayerResponse
import com.vibeon.music.domain.model.SearchResults
import com.vibeon.music.domain.model.Song
import com.vibeon.music.domain.model.StreamUrl
import com.vibeon.music.util.obj
import com.vibeon.music.util.str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class SearchFilter { SONGS, VIDEOS, ALBUMS, ARTISTS, PLAYLISTS }

private fun <T> List<T>.rotatedBy(shift: Int): List<T> {
    if (size <= 1) return this
    val offset = ((shift % size) + size) % size
    if (offset == 0) return this
    return drop(offset) + take(offset)
}

@Singleton
class MusicRepository @Inject constructor(
    private val client: InnerTubeClient,
    private val potMinter: PoTokenMinter,
) {
    private val streamCache = mutableMapOf<String, PlayerResponse>()

    // Increments on every home load so the same chip never renders the exact same
    // set of cards twice, even when YouTube returns a deterministic feed.
    private val homeLoadSequence = AtomicInteger(0)

    data class Queue(
        val songs: List<Song> = emptyList(),
        val currentIndex: Int = -1,
    )

    private val _queue = MutableStateFlow(Queue())
    val queue: StateFlow<Queue> = _queue.asStateFlow()

    suspend fun fetchHome(params: String? = null): HomeSectionResponse? {
        val seed = homeLoadSequence.getAndIncrement()
        val rawHome = client.browse("FEmusic_home", params, freshVisitor = true) ?: return null
        val realVisitor = rawHome.obj("responseContext")?.str("visitorData")
        val home = InnerTubeParser.parseHome(rawHome) ?: return null

        var sections = home.sections
            .filter { it.items.isNotEmpty() || it.kind == HomeSectionKind.TASTEBUILDER }
            .toMutableList()

        if (params == null) {
            // The default feed is personalized and can be sparse for new accounts. Pin a
            // rotating set of curated genre sections on top so Home always shows a rich
            // mix, and so the mix itself changes from one visit to the next.
            try {
                val curated = mutableListOf<HomeSection>()
                val genres = listOf(
                    "Energize", "Romance", "Feel good", "Party", "Relax",
                    "Workout", "Focus", "Sad", "Commute", "Sleep",
                ).rotatedBy(seed)
                var attempts = 0
                for (label in genres) {
                    if (attempts++ >= 4 || curated.size >= 4) break
                    val chip = home.chips.firstOrNull { it.label.equals(label, ignoreCase = true) }
                        ?: continue
                    val genreHome = client.browse("FEmusic_home", chip.params, freshVisitor = true)
                        ?.let { InnerTubeParser.parseHome(it) }
                        ?: continue
                    for (section in genreHome.sections.filter { it.items.isNotEmpty() }.take(2)) {
                        val duplicate = (curated + sections).any {
                            it.title.equals(section.title, ignoreCase = true)
                        }
                        if (!duplicate) curated.add(section)
                    }
                }
                sections = (curated + sections).toMutableList()
            } catch (_: Exception) {
                // Keep the plain personalized feed if the curated lookups fail.
            }
        }

        // YouTube often returns the same items for the same genre params. Rotate the
        // cards inside every section (and the section order) by the load seed so that
        // tapping a chip again or pulling to refresh always surfaces a different mix.
        val refreshed = sections
            .mapIndexed { index, section ->
                section.copy(items = section.items.rotatedBy(seed + index * 7))
            }
            .rotatedBy(seed)
            .distinctBy { it.title }

        return home.copy(sections = refreshed, visitorData = realVisitor)
    }

    suspend fun fetchHomeMore(continuation: String, visitorData: String? = null): HomeSectionResponse? =
        client.continuation(continuation, visitorData)?.let { InnerTubeParser.parseHomeMore(it) }

    /** Explore landing: quick links + mood tiles + featured shelves. */
    suspend fun fetchExplore(): ExploreData? {
        val explore = client.browse("FEmusic_explore", freshVisitor = true) ?: return null
        val moods = client.browse("FEmusic_moods_and_genres", freshVisitor = true)
        val charts = client.browse("FEmusic_charts", freshVisitor = true)
        val sections = buildList {
            fun addSections(body: kotlinx.serialization.json.JsonObject?) {
                body ?: return
                val parsed = InnerTubeParser.parseHome(body) ?: return
                addAll(parsed.sections.filter { it.items.isNotEmpty() })
            }
            addSections(explore)
            addSections(charts)
        }
        return ExploreData(
            tiles = InnerTubeParser.parseExploreTiles(explore) + (moods?.let { InnerTubeParser.parseExploreTiles(it) } ?: emptyList()),
            sections = sections,
        )
    }

    /** A tile destination (charts, moods category, new releases, …) rendered as shelves. */
    suspend fun fetchExploreCategory(browseId: String, params: String?): ExploreData? {
        val body = client.browse(browseId, params, freshVisitor = true) ?: return null
        val parsed = InnerTubeParser.parseHome(body)
        return ExploreData(
            tiles = InnerTubeParser.parseExploreTiles(body),
            sections = parsed?.sections?.filter { it.items.isNotEmpty() } ?: emptyList(),
        )
    }

    suspend fun search(query: String, filter: SearchFilter = SearchFilter.SONGS): SearchResults {
        val params = when (filter) {
            SearchFilter.SONGS -> null
            SearchFilter.VIDEOS -> "EgWKAQIQAWoKEAoQCRADEAA%3D%3D"
            SearchFilter.ALBUMS -> "EgWKAQIYAWoKEAoQCRADEAA%3D%3D"
            SearchFilter.ARTISTS -> "EgWKAQIKAWoKEAoQCRADEAA%3D%3D"
            SearchFilter.PLAYLISTS -> "EgWKAQILAWoKEAoQCRADEAA%3D%3D"
        }
        return client.search(query, params)?.let { InnerTubeParser.parseSearch(it) } ?: SearchResults()
    }

    suspend fun searchMore(continuation: String): SearchResults =
        client.continuation(continuation)?.let { InnerTubeParser.parseSearchMore(it) } ?: SearchResults()

    suspend fun fetchAlbum(browseId: String): AlbumResponse? =
        client.browse(browseId)?.let { InnerTubeParser.parseAlbum(it, browseId) }

    suspend fun fetchPlaylist(browseId: String): PlaylistResponse? =
        client.browse(browseId)?.let { InnerTubeParser.parsePlaylist(it, browseId) }

    suspend fun fetchArtist(browseId: String): ArtistResponse? =
        client.browse(browseId)?.let { InnerTubeParser.parseArtist(it, browseId) }

    suspend fun fetchSongsContinuation(continuation: String): Pair<List<Song>, String?> =
        client.continuation(continuation)?.let { InnerTubeParser.parseTrackContinuation(it) }
            ?: (emptyList<Song>() to null)

    suspend fun getStreams(song: Song, attempt: Int = 0): PlayerResponse? {
        streamCache[song.videoId]?.let { return it }
        var minted = potMinter.get()
        var body = client.player(song.videoId, minted)
        // The ANDROID client needs its stream URLs to carry the poToken too; if the
        // server rejected the token, invalidate and try once more.
        if (body == null && minted != null) {
            potMinter.invalidate()
            minted = potMinter.get()
            body = client.player(song.videoId, minted)
        }
        body ?: return null
        val data = InnerTubeParser.parsePlayer(body)
        val response = PlayerResponse(
            song = data.song ?: song,
            streams = if (minted != null) {
                data.streams.map { s ->
                    s.copy(url = appendPot(s.url, minted.pot))
                }
            } else {
                data.streams
            },
            playabilityStatus = data.playabilityStatus,
            playabilityError = data.error,
        )
        if (data.playabilityStatus == "OK") {
            streamCache[song.videoId] = response
        } else if (data.playabilityStatus != "OK" && minted != null && attempt == 0) {
            // A stale/consumed token shows up as an unplayable response rather than a
            // missing body; re-mint once so the next queued track still loads.
            potMinter.invalidate()
            streamCache.remove(song.videoId)
            return getStreams(song, attempt = 1)
        }
        return response
    }

    private fun appendPot(url: String, pot: String): String {
        val sep = if (url.contains("?")) "&" else "?"
        return "$url${sep}pot=$pot"
    }

    fun pickStream(streams: List<StreamUrl>, quality: AudioQuality): StreamUrl? {
        if (streams.isEmpty()) return null
        return when (quality) {
            AudioQuality.LOW -> streams
                .filter { it.bitrate in 48_000..160_000 }
                .minByOrNull { it.bitrate }
                ?: streams.minByOrNull { it.bitrate }

            AudioQuality.HIGH -> streams.maxByOrNull { it.bitrate }

            AudioQuality.NORMAL -> streams
                .sortedBy { it.bitrate }
                .firstOrNull { it.bitrate >= 128_000 }
                ?: streams.maxByOrNull { it.bitrate }
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.value = Queue(songs, startIndex)
    }

    fun updateQueueIndex(index: Int) {
        _queue.value = _queue.value.copy(currentIndex = index)
    }

    fun currentQueue(): Queue = _queue.value
}