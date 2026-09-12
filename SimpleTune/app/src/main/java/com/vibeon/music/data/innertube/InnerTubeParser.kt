package com.vibeon.music.data.innertube

import com.vibeon.music.core.Constants
import com.vibeon.music.domain.model.Album
import com.vibeon.music.domain.model.Artist
import com.vibeon.music.domain.model.BrowseItem
import com.vibeon.music.domain.model.HomeChip
import com.vibeon.music.domain.model.HomeSection
import com.vibeon.music.domain.model.HomeSectionLayout
import com.vibeon.music.domain.model.Image
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.domain.model.Playlist
import com.vibeon.music.domain.model.SearchResults
import com.vibeon.music.domain.model.Song
import com.vibeon.music.domain.model.StreamUrl
import com.vibeon.music.util.arr
import com.vibeon.music.util.deepFind
import com.vibeon.music.util.int
import com.vibeon.music.util.obj
import com.vibeon.music.util.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object InnerTubeParser {
    private const val TAG = "InnerTubeParser"

    // ---------- thumbnails ----------

    fun thumbnails(json: JsonObject?): List<Image> {
        val renderer = json?.obj("musicThumbnailRenderer")
            ?: (json?.deepFind("musicThumbnailRenderer") as? JsonObject)
            ?: return emptyList()
        val list = renderer.arr("thumbnail", "thumbnails") ?: return emptyList()
        return list.mapNotNull { item ->
            val o = (item as? JsonObject) ?: return@mapNotNull null
            val url = o.str("url") ?: return@mapNotNull null
            val width = o.int("width")?.toInt() ?: 0
            val height = o.int("height")?.toInt() ?: 0
            Image(url, width, height)
        }
    }

    fun thumbnailUrl(json: JsonObject?, videoIdFallback: String? = null): String? {
        val thumbs = thumbnails(json)
        if (thumbs.isNotEmpty()) {
            val target = thumbs.map { it }.sortedBy { kotlin.math.abs(it.width - 300) }.firstOrNull()
            target?.let { return target.url }
        }
        return videoIdFallback?.let { Constants.thumbnailUrl(it) }
    }

    // ---------- text helpers ----------

    private fun runsText(runs: JsonArray?): String =
        runs?.mapNotNull { (it as? JsonObject)?.str("text") }?.joinToString("") ?: ""

    private fun titleOf(json: JsonObject?): String? {
        json ?: return null
        val title = json.deepFind("runs")?.let { it as? JsonArray }?.let { runsText(it) }
        return title?.takeIf { it.isNotBlank() } ?: json.str("title")
    }

    private fun flexText(item: JsonObject, column: Int): String? {
        val col = (item.arr("flexColumns")?.getOrNull(column) as? JsonObject)
            ?.obj("musicResponsiveListItemFlexColumnRenderer") ?: return null
        return col.let {
            (it.arr("text", "runs") ?: it.arr("text"))?.let { runs -> runsText(runs) }
        }
    }

    private fun fixedText(item: JsonObject, column: Int): String? {
        val col = (item.arr("fixedColumns")?.getOrNull(column) as? JsonObject)
            ?.obj("musicResponsiveListItemFixedColumnRenderer") ?: return null
        return (col.arr("text", "runs") ?: col.arr("text"))?.let { runs -> runsText(runs) }
    }

    fun parseDuration(text: String?): Long {
        val t = text?.trim() ?: return 0
        val parts = t.split(":").mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty()) return 0
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    private fun navigationOf(item: JsonObject): Pair<String?, String?> {
        val nav = item.deepFind("navigationEndpoint")?.let { it as? JsonObject } ?: return null to null
        val videoId = nav.str("watchEndpoint", "videoId")
        val browseId = nav.str("browseEndpoint", "browseId")
        return videoId to browseId
    }

    private fun songFromItem(item: JsonObject, fallbackVideoId: String? = null): Song? {
        val title = flexText(item, 0) ?: return null
        if (title.isBlank() || title.equals("Play next", ignoreCase = true)) return null
        val (videoId, _) = navigationOf(item)
        val id = videoId ?: fallbackVideoId ?: return null
        val runs = (item.arr("flexColumns")?.getOrNull(1) as? JsonObject)
            ?.obj("musicResponsiveListItemFlexColumnRenderer")?.arr("text", "runs")
        val texts = runs?.mapNotNull { (it as? JsonObject)?.str("text") } ?: emptyList()
        val artistName = texts.firstOrNull { it.isNotBlank() && it != " • " }
        val albumName = texts.firstOrNull { it.contains("Album") && it != " • " && it != artistName }
            ?.replace("Album", "")?.trim()
        val duration = parseDuration(fixedText(item, 0))
        return Song(
            videoId = id,
            title = title,
            artists = artistName?.let { listOf(Artist(name = it)) } ?: emptyList(),
            album = if (!albumName.isNullOrBlank()) Album(title = albumName) else null,
            duration = duration,
            thumbnailUrl = thumbnailUrl(item, id),
        )
    }

    private fun itemTypeOf(browseId: String?): ItemType? {
        browseId ?: return null
        return when {
            browseId.startsWith("MPREb_") -> ItemType.ALBUM
            browseId.startsWith("VL") || browseId.startsWith("RDCLAK") -> ItemType.PLAYLIST
            browseId.startsWith("UC") -> ItemType.ARTIST
            browseId.startsWith("RDAMVM") || browseId.startsWith("RDAMPL") -> ItemType.RADIO
            else -> null
        }
    }

    private fun browseItemFromTwoRow(item: JsonObject): BrowseItem? {
        val title = item.str("title", "runs") ?: titleOf(item) ?: return null
        val subtitle = runsText(item.arr("subtitle", "runs"))
        val (videoId, browseId) = navigationOf(item)
        val type = itemTypeOf(browseId) ?: if (videoId != null) ItemType.VIDEO else ItemType.UNKNOWN
        val year = subtitle?.let { it.trim().toIntOrNull() }
        return BrowseItem(
            type = type,
            title = title,
            subtitle = subtitle ?: "",
            videoId = videoId,
            browseId = browseId,
            thumbnailUrl = thumbnailUrl(item),
            year = year,
        )
    }

    private fun browseItemFromResponsive(item: JsonObject): BrowseItem? {
        val title = flexText(item, 0) ?: return null
        if (title.isBlank()) return null
        val (videoId, browseId) = navigationOf(item)
        if (videoId == null && browseId == null) return null
        val subtitle = (flexText(item, 1) ?: "").replace(" • ", " · ")
        val type = itemTypeOf(browseId) ?: if (videoId != null) ItemType.SONG else ItemType.UNKNOWN
        return BrowseItem(
            type = type,
            title = title,
            subtitle = subtitle,
            videoId = videoId,
            browseId = browseId,
            thumbnailUrl = thumbnailUrl(item, videoId),
        )
    }

    internal fun browseItemOf(item: JsonObject): BrowseItem? {
        item.obj("musicTwoRowItemRenderer")?.let { return browseItemFromTwoRow(it) }
        item.obj("musicResponsiveListItemRenderer")?.let { return browseItemFromResponsive(it) }
        return null
    }

    internal fun songOf(item: JsonObject): Song? {
        item.obj("musicResponsiveListItemRenderer")?.let { return songFromItem(it) }
        return null
    }

    private fun flattenContents(contents: JsonArray?): List<JsonObject> {
        contents ?: return emptyList()
        val out = mutableListOf<JsonObject>()
        for (element in contents) {
            val o = element as? JsonObject ?: continue
            val multi = o.obj("musicMultiRowBrowseResultsRenderer")?.arr("contents")
            if (multi != null) {
                out.addAll(flattenContents(multi))
            } else {
                out.add(o)
            }
        }
        return out
    }

    private fun headerTitle(header: JsonObject?): String? {
        header ?: return null
        val renderers = listOf(
            "musicCarouselShelfBasicHeaderRenderer",
            "musicCarouselShelfHeaderRenderer",
            "musicResponsiveListHeaderRenderer",
            "musicDetailHeaderRenderer",
            "musicPlaylistHeaderRenderer",
            "musicEditablePlaylistDetailHeaderRenderer",
        )
        for (r in renderers) {
            header.obj(r)?.let { renderer ->
                val t = runsText(renderer.arr("title", "runs")).takeIf { it.isNotBlank() }
                    ?: renderer.str("straplineText", "runs")
                if (!t.isNullOrBlank()) return t
            }
        }
        return null
    }

    // ---------- Home ----------

    fun parseHome(body: JsonObject): HomeSectionResponse? {
        val tabRenderer = (body.obj("contents")?.obj("singleColumnBrowseResultsRenderer")
            ?.arr("tabs")?.firstOrNull() as? JsonObject)?.obj("tabRenderer") ?: return null
        val list = tabRenderer.obj("content")?.obj("sectionListRenderer") ?: return null
        val sections = list.arr("contents")
            ?.mapNotNull { section -> homeSectionOf(section as? JsonObject ?: return@mapNotNull null) }
            ?: emptyList()
        val chips = parseChips(list.obj("header"))
        val more = continuationOf(list.arr("continuations"))
        return HomeSectionResponse(sections, more, chips)
    }

    private fun parseChips(header: JsonObject?): List<HomeChip> {
        header ?: return emptyList()
        val chips = header.obj("chipCloudRenderer")?.arr("chips") ?: return emptyList()
        return chips.mapNotNull { element ->
            val chip = (element as? JsonObject)?.obj("chipCloudChipRenderer") ?: return@mapNotNull null
            val label = runsText(chip.arr("text", "runs")).takeIf { it.isNotBlank() }
                ?: chip.str("title")
                ?: return@mapNotNull null
            val endpoint = chip.obj("navigationEndpoint") ?: return@mapNotNull null
            val browseId = endpoint.str("browseEndpoint", "browseId") ?: return@mapNotNull null
            HomeChip(
                label = label,
                browseId = browseId,
                params = endpoint.str("browseEndpoint", "params"),
            )
        }
    }

    private fun homeSectionOf(section: JsonObject): HomeSection? {
        section.obj("musicCarouselShelfRenderer")?.let { carousel ->
            val title = headerTitle(carousel.obj("header")) ?: return null
            val items = carouselItems(carousel.arr("contents"))
            if (items.isEmpty()) return null
            val more = continuationOf(carousel.arr("continuations"))
            return HomeSection(title, items, more)
        }
        section.obj("musicImmersiveCarouselShelfRenderer")?.let { carousel ->
            val title = headerTitle(carousel.obj("header")) ?: return null
            val items = carouselItems(carousel.arr("contents"))
            if (items.isEmpty()) return null
            return HomeSection(title, items, null)
        }
        section.obj("musicResponsiveListRenderer")?.let { list ->
            val title = headerTitle(list.obj("header")) ?: return null
            val items = flattenContents(list.arr("contents")).mapNotNull { browseItemOf(it) }
            if (items.isEmpty()) return null
            val more = continuationOf(list.arr("continuations"))
            val layout = if (items.all { it.type == ItemType.SONG && it.videoId != null }) {
                HomeSectionLayout.LIST
            } else {
                HomeSectionLayout.GRID
            }
            return HomeSection(title, items, more, layout)
        }
        section.obj("musicMultiRowBrowseResultsRenderer")?.let { multi ->
            val title = headerTitle(multi.obj("header")) ?: return null
            val items = flattenContents(multi.arr("contents")).mapNotNull { browseItemOf(it) }
            if (items.isEmpty()) return null
            val more = continuationOf(multi.arr("continuations"))
            return HomeSection(title, items, more, HomeSectionLayout.GRID)
        }
        section.obj("musicShelfRenderer")?.let { shelf ->
            val content = shelf.obj("content") ?: return null
            val list = content.obj("musicResponsiveListRenderer")
                ?: content.obj("musicPlaylistShelfRenderer")
                ?: return null
            val title = headerTitle(shelf.obj("header")) ?: return null
            val items = flattenContents(list.arr("contents")).mapNotNull { browseItemOf(it) }
            if (items.isEmpty()) return null
            val more = continuationOf(list.arr("continuations"))
            return HomeSection(title, items, more, HomeSectionLayout.LIST)
        }
        section.obj("gridRenderer")?.let { grid ->
            val items = grid.arr("items").orEmpty()
                .mapNotNull { (it as? JsonObject)?.let { o -> browseItemOf(o) } }
            if (items.isEmpty()) return null
            return HomeSection("", items, null, HomeSectionLayout.GRID)
        }
        return null
    }

    private fun carouselItems(contents: JsonArray?): List<BrowseItem> =
        flattenContents(contents).mapNotNull { browseItemOf(it) }

    private fun continuationOf(continuations: JsonArray?): String? =
        continuations?.firstOrNull()?.obj("nextContinuationData")?.str("continuation")

    fun parseHomeMore(body: JsonObject): HomeSectionResponse? {
        val container = body.obj("continuationContents") ?: return null
        val theList = container.obj("musicCarouselShelfRenderer")
        val list = theList
            ?: container.obj("musicResponsiveListRenderer")
            ?: container.obj("musicMultiRowBrowseResultsRenderer")
            ?: return null
        val title = headerTitle(list.obj("header"))
        val items = flattenContents(list.arr("contents")).mapNotNull { browseItemOf(it) }
        val more = continuationOf(list.arr("continuations"))
        val layout = if (theList != null) {
            HomeSectionLayout.CAROUSEL
        } else if (items.all { it.type == ItemType.SONG && it.videoId != null }) {
            HomeSectionLayout.LIST
        } else {
            HomeSectionLayout.GRID
        }
        return HomeSectionResponse(listOf(HomeSection(title ?: "More", items, more, layout)), more, emptyList())
    }

    // ---------- Search ----------

    fun parseSearch(body: JsonObject): SearchResults {
        val contents: JsonArray = body.obj("contents")?.let { root ->
            root.obj("tabbedSearchResultsRenderer")?.arr("tabs")?.firstOrNull()
                ?.arr("tabRenderer", "content", "sectionListRenderer", "contents")
                ?: root.obj("sectionListRenderer")?.arr("contents")
        } ?: return SearchResults()
        return parseSearchContents(contents)
    }

    private fun parseSearchContents(contents: JsonArray): SearchResults {
        val songs = mutableListOf<Song>()
        val videos = mutableListOf<Song>()
        val albums = mutableListOf<Album>()
        val artists = mutableListOf<Artist>()
        val playlists = mutableListOf<Playlist>()
        var songsContinuation: String? = null

        for (element in contents) {
            val section = element as? JsonObject ?: continue
            section.obj("musicShelfRenderer")?.let { shelf ->
                val content = shelf.obj("content") ?: return@let
                content.obj("musicResponsiveListRenderer")?.let { list ->
                    for (row in flattenContents(list.arr("contents"))) {
                        val r = row.obj("musicResponsiveListItemRenderer")
                        if (r == null) continue
                        val (videoId, browseId) = navigationOf(r)
                        if (videoId != null && flexText(r, 0) != null) {
                            songs.add(songFromItem(r) ?: continue)
                        } else if (browseId != null) {
                            addBrowseShelf(r, albums, artists, playlists)
                        }
                    }
                    songsContinuation = continuationOf(list.arr("continuations"))
                }
                content.obj("musicPlaylistShelfRenderer")?.let { list ->
                    for (row in flattenContents(list.arr("contents"))) {
                        songs.add(songOf(row) ?: continue)
                    }
                    songsContinuation = continuationOf(list.arr("continuations"))
                }
            }
            section.obj("itemSectionRenderer")?.let { isr ->
                for (row in flattenContents(isr.arr("contents"))) {
                    val r = row.obj("musicResponsiveListItemRenderer") ?: continue
                    val (videoId, browseId) = navigationOf(r)
                    if (videoId != null && flexText(r, 0) != null) {
                        songs.add(songFromItem(r) ?: continue)
                    } else if (browseId != null) {
                        addBrowseShelf(r, albums, artists, playlists)
                    }
                }
            }
            section.obj("musicCardShelfRenderer")?.let { card ->
                card.arr("contents")?.forEach { row ->
                    (row as? JsonObject)?.obj("musicResponsiveListItemRenderer")
                        ?.let { r -> songFromItem(r)?.let { songs.add(0, it) } }
                } ?: card.obj("content", "musicResponsiveListItemRenderer")?.let { row ->
                    songFromItem(row)?.let { songs.add(0, it) }
                }
            }
        }

        // Two-row based albums/artists/playlists can be in a carousel shelf.
        for (element in contents) {
            val section = element as? JsonObject ?: continue
            val list = section.obj("musicCarouselShelfRenderer")
            if (list != null) {
                for (row in flattenContents(list.arr("contents"))) {
                    val r = row.obj("musicTwoRowItemRenderer") ?: continue
                    browseItemFromTwoRow(r)?.let { item ->
                        when (item.type) {
                            ItemType.ALBUM -> albums.add(
                                Album(
                                    id = item.browseId.orEmpty(),
                                    title = item.title,
                                    artists = item.subtitle.split(" · ").filter { it.isNotBlank() }
                                        .map { Artist(name = it) },
                                    thumbnailUrl = item.thumbnailUrl,
                                    year = item.year,
                                )
                            )
                            ItemType.PLAYLIST -> playlists.add(
                                Playlist(
                                    id = item.browseId.orEmpty(),
                                    title = item.title,
                                    author = item.subtitle,
                                    thumbnailUrl = item.thumbnailUrl,
                                )
                            )
                            ItemType.ARTIST -> artists.add(
                                Artist(
                                    id = item.browseId.orEmpty(),
                                    name = item.title,
                                    thumbnailUrl = item.thumbnailUrl,
                                )
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }

        return SearchResults(songs, videos, albums, artists, playlists)
    }

    private fun addBrowseShelf(
        r: JsonObject,
        albums: MutableList<Album>,
        artists: MutableList<Artist>,
        playlists: MutableList<Playlist>,
    ) {
        val (_, browseId) = navigationOf(r)
        val title = flexText(r, 0) ?: return
        val subtitle = (flexText(r, 1) ?: "").replace(" • ", " · ")
        when (itemTypeOf(browseId)) {
            ItemType.ALBUM -> albums.add(
                Album(browseId.orEmpty(), title, subtitle.split(" · ").map { Artist(name = it) }, thumbnailUrl(r))
            )
            ItemType.ARTIST -> artists.add(Artist(browseId.orEmpty(), title, thumbnailUrl(r)))
            ItemType.PLAYLIST -> playlists.add(Playlist(browseId.orEmpty(), title, subtitle, thumbnailUrl(r)))
            else -> Unit
        }
    }

    fun parseSearchMore(body: JsonObject): SearchResults {
        val songs = mutableListOf<Song>()
        val list = body.obj("continuationContents")?.obj("musicResponsiveListRenderer")
        list?.let {
            for (row in flattenContents(it.arr("contents"))) songs.add(songOf(row) ?: continue)
        }
        return SearchResults(songs = songs)
    }

    fun parseAlbum(body: JsonObject, browseId: String): AlbumResponse {
        val detail = body.obj("header")?.obj("musicDetailHeaderRenderer")
        val title = titleOf(detail) ?: ""
        val subtitleParts = detail?.deepFind("runs")?.let { it as? JsonArray }
            ?.mapNotNull { (it as? JsonObject)?.str("text") } ?: emptyList()
        val year = subtitleParts.firstNotNullOfOrNull { it.trim().toIntOrNull() }
        val artistNames = subtitleParts.filterNot {
            it.isBlank() || it.trim().toIntOrNull() != null || it == " • " || it.equals("Album", true)
        }
        val songCount = subtitleParts.firstNotNullOfOrNull { s ->
            Regex("(\\d+) song").find(s)?.groupValues?.get(1)?.toInt()
        }

        val album = Album(
            id = browseId,
            title = title,
            artists = artistNames.map { Artist(name = it) },
            thumbnailUrl = thumbnailUrl(detail),
            year = year,
            songCount = songCount,
        )

        val (songs, continuation) = parseTrackShelf(body)
        return AlbumResponse(album, songs, continuation)
    }

    fun parsePlaylist(body: JsonObject, browseId: String): PlaylistResponse {
        val header = body.obj("header")?.obj("musicPlaylistHeaderRenderer")
        val title = titleOf(header) ?: ""
        val author = runsText(header?.arr("subtitle", "runs"))
        val songCount = header?.deepFind("runs")?.let { it as? JsonArray }
            ?.firstNotNullOfOrNull { json ->
                json.let { (it as? JsonObject)?.str("text") }?.let { text ->
                    Regex("(\\d+) song").find(text)?.groupValues?.get(1)?.toInt()
                }
            }
        val playlists = Playlist(
            id = browseId,
            title = title,
            author = author,
            thumbnailUrl = thumbnailUrl(header),
            songCount = songCount,
            description = runsText(header?.arr("description", "runs")),
        )
        val (songs, continuation) = parseTrackShelf(body)
        return PlaylistResponse(playlists, songs, continuation)
    }

    private fun parseTrackShelf(body: JsonObject): Pair<List<Song>, String?> {
        val contents = body.obj("contents")
        val candidates = mutableListOf<JsonObject>()
        contents?.obj("twoColumnBrowseResultsRenderer")?.let { r ->
            candidates += r.arr("tabs")?.firstOrNull()?.arr("tabRenderer", "content", "sectionListRenderer", "contents")
                .orEmpty().filterIsInstance<JsonObject>()
            candidates += r.arr("secondaryContents", "sectionListRenderer", "contents")
                .orEmpty().filterIsInstance<JsonObject>()
        }
        contents?.obj("singleColumnBrowseResultsRenderer")?.let { r ->
            candidates += r.arr("tabs")?.firstOrNull()?.arr("tabRenderer", "content", "sectionListRenderer", "contents")
                .orEmpty().filterIsInstance<JsonObject>()
        }

        candidates.firstNotNullOfOrNull { it.obj("musicPlaylistShelfRenderer") }?.let { shelf ->
            val songs = flattenContents(shelf.arr("contents")).mapNotNull { songOf(it) }
            return songs to continuationOf(shelf.arr("continuations"))
        }

        candidates.firstNotNullOfOrNull { it.obj("musicResponsiveListRenderer") }?.let { responsive ->
            val songs = flattenContents(responsive.arr("contents")).mapNotNull { songOf(it) }
            return songs to continuationOf(responsive.arr("continuations"))
        }
        return emptyList<Song>() to null
    }

    fun parseTrackContinuation(body: JsonObject): Pair<List<Song>, String?> {
        val shelf = body.obj("continuationContents")?.obj("musicPlaylistShelfRenderer")
        if (shelf != null) {
            val songs = flattenContents(shelf.arr("contents")).mapNotNull { songOf(it) }
            return songs to continuationOf(shelf.arr("continuations"))
        }
        val responsive = body.obj("continuationContents")?.obj("musicResponsiveListRenderer")
        if (responsive != null) {
            val songs = flattenContents(responsive.arr("contents")).mapNotNull { songOf(it) }
            return songs to continuationOf(responsive.arr("continuations"))
        }
        return emptyList<Song>() to null
    }

    fun parseArtist(body: JsonObject, browseId: String): ArtistResponse {
        val detail = body.obj("header")?.obj("musicImmersiveHeaderRenderer")
        val artist = Artist(
            id = browseId,
            name = titleOf(detail) ?: "",
            thumbnailUrl = thumbnailUrl(detail),
        )
        val songs = mutableListOf<Song>()
        body.obj("contents")?.obj("singleColumnBrowseResultsRenderer")?.arr("tabs")?.firstOrNull()
            ?.obj("tabRenderer", "content", "sectionListRenderer", "contents")?.forEach { section ->
                (section as? JsonObject)?.obj("musicResponsiveListRenderer")?.let { list ->
                    val title = headerTitle(list.obj("header"))
                    if (title == null || title.contains("Album", true) || title.contains("Single", true)) return@let
                    for (row in flattenContents(list.arr("contents"))) {
                        songOf(row)?.let { songs.add(it) }
                    }
                }
                (section as? JsonObject)?.obj("musicShelfRenderer")?.let { shelf ->
                    for (row in flattenContents(shelf.obj("content")?.obj("musicResponsiveListRenderer")?.arr("contents"))) {
                        songOf(row)?.let { songs.add(it) }
                    }
                }
            }
        return ArtistResponse(artist, songs)
    }

    // ---------- Player ----------

    fun parsePlayer(body: JsonObject): PlayerData {
        val status = body.obj("playabilityStatus")?.str("status") ?: "UNKNOWN"
        if (status != "OK") {
            return PlayerData(
                playabilityStatus = status,
                error = body.obj("playabilityStatus")?.str("reason")
                    ?: body.obj("playabilityStatus")?.obj("errorScreen")?.obj("playerErrorMessageRenderer")
                        ?.str("reason")
                    ?: "Playback unavailable",
            )
        }

        val details = body.obj("videoDetails")
        val videoId = details?.str("videoId") ?: return PlayerData(playabilityStatus = "ERROR")
        val title = details?.str("title") ?: ""
        val author = details?.str("author") ?: ""
        val authorId = details?.str("channelId")
        val duration = details?.int("lengthSeconds") ?: 0
        var thumbnailUrl = details?.obj("thumbnail")?.arr("thumbnails")
            ?.mapNotNull { m -> (m as? JsonObject)?.str("url") }
            ?.lastOrNull()
            ?: details?.deepFind("thumbnailUrl")?.let { (it as? JsonPrimitive)?.contentOrNull }
        if (thumbnailUrl.isNullOrBlank()) thumbnailUrl = Constants.thumbnailUrl(videoId)

        val song = Song(
            videoId = videoId,
            title = title,
            artists = if (author.isNotBlank()) listOf(Artist(authorId.orEmpty(), author)) else emptyList(),
            duration = duration,
            thumbnailUrl = thumbnailUrl,
        )

        val progressiveFormats = body.obj("streamingData")?.arr("formats") ?: emptyList()
        val adaptiveFormats = body.obj("streamingData")?.arr("adaptiveFormats") ?: emptyList()
        val streams = (progressiveFormats + adaptiveFormats).mapNotNull { format ->
            val o = format as? JsonObject ?: return@mapNotNull null
            val mime = o.str("mimeType") ?: return@mapNotNull null
            val codecsPart = mime.substringAfter("codecs=", "").trim('"', ' ')
            val hasAudio = mime.startsWith("audio/") ||
                codecsPart.contains("mp4a") || codecsPart.contains("opus") ||
                codecsPart.contains("mp3") || codecsPart.contains("aac")
            if (!hasAudio) return@mapNotNull null
            val url = o.str("url") ?: parseCipher(o.str("signatureCipher") ?: o.str("cipher"))
            if (url.isNullOrBlank()) return@mapNotNull null
            // Progressive (muxed) formats like itag 18 carry aac audio inside an mp4
            // video container; ExoPlayer accepts them as audio sources too.
            val audioOnly = mime.startsWith("audio/")
            StreamUrl(
                url = url,
                mimeType = if (audioOnly) mime.substringBefore(";") else "audio/mp4",
                bitrate = o.int("bitrate")?.toInt() ?: 0,
                codec = if (audioOnly) codecsPart else "mp4a.40.2",
                durationMs = o.int("approxDurationMs") ?: 0,
            )
        }
        return PlayerData(status, song, streams)
    }

    private fun parseCipher(cipher: String?): String? {
        if (cipher.isNullOrBlank()) return null
        var url: String? = null
        var sig: String? = null
        var sp = "signature"
        for (part in cipher.split("&")) {
            val idx = part.indexOf('=')
            if (idx <= 0) continue
            val key = decode(part.substring(0, idx))
            val value = decode(part.substring(idx + 1))
            when (key) {
                "url" -> url = value
                "s" -> sig = value
                "sp" -> sp = value
            }
        }
        url ?: return null
        return if (!sig.isNullOrBlank()) {
            val sep = if (url.contains("?")) "&" else "?"
            "$url$sep$sp=$sig"
        } else {
            url
        }
    }

    private fun decode(s: String): String =
        try {
            java.net.URLDecoder.decode(s, "UTF-8")
        } catch (e: Exception) {
            s
        }
}

data class AlbumResponse(val album: Album, val songs: List<Song>, val continuation: String?)
data class PlaylistResponse(val playlist: Playlist, val songs: List<Song>, val continuation: String?)
data class ArtistResponse(val artist: Artist, val songs: List<Song>)
data class HomeSectionResponse(
    val sections: List<HomeSection>,
    val continuation: String?,
    val chips: List<HomeChip> = emptyList(),
)
data class PlayerData(
    val playabilityStatus: String,
    val song: Song? = null,
    val streams: List<StreamUrl> = emptyList(),
    val error: String? = null,
)