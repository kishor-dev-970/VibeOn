package com.vibeon.music.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUser(
    val id: String,
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val code: String = "",
    val avatarUrl: String? = null,
    val lastActive: Long? = null,
)

@Serializable
data class NowPlaying(
    val isPlaying: Boolean = false,
    val songTitle: String? = null,
    val artistName: String? = null,
    val artworkUrl: String? = null,
    val videoId: String? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class FriendActivity(
    val user: AppUser,
    val nowPlaying: NowPlaying? = null,
)

@Serializable
data class SongStat(
    val songTitle: String,
    val artistName: String? = null,
    val artworkUrl: String? = null,
    val count: Long,
)

@Serializable
data class RecentPlay(
    val songTitle: String,
    val artistName: String? = null,
    val artworkUrl: String? = null,
    val playedAt: Long,
)

@Serializable
data class FriendStats(
    val totalSongs: Long = 0,
    val totalPlays: Long = 0,
    val estimatedMinutes: Long = 0,
    val nowPlaying: NowPlaying? = null,
    val topSongs: List<SongStat> = emptyList(),
    val recentPlays: List<RecentPlay> = emptyList(),
)

fun AppUser.displayName(): String =
    name.ifBlank {
        listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email }
    }