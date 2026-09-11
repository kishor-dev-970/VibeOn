package com.vibeon.music.data.innertube

import android.content.Context
import com.vibeon.music.util.TubeJson
import com.vibeon.music.util.arr
import com.vibeon.music.util.int
import com.vibeon.music.util.obj
import com.vibeon.music.util.str
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerEntry(
    val playerId: String,
    val sig: String?,
    val nClass: String?,
    val sts: Long?,
    val aliases: List<String> = emptyList(),
)

@Singleton
class PlayerConfigLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val configs: Map<String, PlayerEntry> by lazy {
        loadConfigs()
    }

    private fun loadConfigs(): Map<String, PlayerEntry> {
        return try {
            val jsonString = context.assets.open("player_configs.json").bufferedReader().use { it.readText() }
            val root = TubeJson.parseToJsonElement(jsonString).jsonObject
            val players = root.obj("players") ?: return emptyMap()
            val map = mutableMapOf<String, PlayerEntry>()
            for ((key, value) in players) {
                val obj = value as? JsonObject ?: continue
                val entry = PlayerEntry(
                    playerId = key,
                    sig = obj.str("sig"),
                    nClass = obj.str("nClass"),
                    sts = obj.int("sts"),
                    aliases = obj.arr("aliases")?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
                )
                map[key] = entry
                entry.aliases.forEach { alias -> map[alias] = entry }
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getPlayer(playerId: String): PlayerEntry? = configs[playerId]

    val defaultPlayer: PlayerEntry?
        get() = configs.values.firstOrNull()
}
