package com.vibeon.music.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.vibeon.music.BuildConfig
import com.vibeon.music.data.settings.AudioQuality
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnBgCardLight
import com.vibeon.music.ui.theme.VibeOnBorder
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnSuccess
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted
import com.vibeon.music.ui.theme.VibeOnTextSubtle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private const val GITHUB_LINK = "https://github.com/kishor-dev-970/VibeOn/releases/latest"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()
    val audioQuality by viewModel.audioQuality.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsState()
    val busySignOut by viewModel.busySignOut.collectAsState()
    val checkingUpdate by viewModel.checkingUpdate.collectAsState()
    val context = LocalContext.current

    var showSignOutDialog by remember { mutableStateOf(false) }
    var updateDialog by remember { mutableStateOf<UpdateDialog?>(null) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = VibeOnText)
                }
                Text(
                    text = "Settings",
                    modifier = Modifier.weight(1f),
                    color = VibeOnText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // MY PROFILE
                SectionLabel("MY PROFILE")
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val name = user?.name ?: "You"
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(VibeOnPrimary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initials(name),
                                color = Color.Black,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = name,
                                color = VibeOnText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "User code: ${user?.code?.ifBlank { "—" } ?: "—"}",
                                color = VibeOnTextMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                // INVITE FRIENDS
                SectionLabel("INVITE FRIENDS")
                SettingsCard {
                    SettingRow(
                        icon = "📣",
                        label = "Invite Friends",
                        onClick = {
                            val message = "Join me on VibeOn! My user code is ${user?.code ?: ""}. Listen to music together. Download the app: $GITHUB_LINK"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            ContextCompat.startActivity(context, Intent.createChooser(intent, "Invite to VibeOn"), null)
                        },
                    )
                    RowDivider()
                    SettingRow(
                        icon = "💬",
                        label = "Share on WhatsApp",
                        onClick = {
                            val msg = Uri.encode("Join me on VibeOn! My user code is ${user?.code ?: ""}. Listen to music together. Download the app: $GITHUB_LINK")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?text=$msg"))
                            runCatching { context.startActivity(intent) }
                        },
                    )
                }

                // PLAYBACK
                SectionLabel("PLAYBACK")
                SettingsCard {
                    Text(
                        text = "Audio quality",
                        color = VibeOnText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioQuality.entries.forEach { quality ->
                            val selected = audioQuality == quality
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) VibeOnPrimary else VibeOnBgCardLight,
                                        RoundedCornerShape(50),
                                    )
                                    .clickable { viewModel.setAudioQuality(quality) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = quality.label(),
                                    color = if (selected) Color.Black else VibeOnTextMuted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // SLEEP TIMER
                SectionLabel("SLEEP TIMER")
                SettingsCard {
                    Text(
                        text = if (sleepTimerMinutes != null && sleepTimerRemainingSec != null)
                            "Active · Stops playback in ${"%.0f".format(sleepTimerRemainingSec!! / 60f)} min"
                        else
                            "Automatically pause playback after set duration",
                        color = if (sleepTimerMinutes != null) VibeOnSuccess else VibeOnTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to "Off",
                            15 to "15m",
                            30 to "30m",
                            45 to "45m",
                            60 to "60m",
                        ).forEach { (minutes, label) ->
                            val selected = (minutes == 0 && sleepTimerMinutes == null) || sleepTimerMinutes == minutes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) VibeOnPrimary else VibeOnBgCardLight,
                                        RoundedCornerShape(50),
                                    )
                                    .clickable { viewModel.setSleepTimer(if (minutes == 0) null else minutes) }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.Black else VibeOnTextMuted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // ACCOUNT
                SectionLabel("ACCOUNT")
                SettingsCard {
                    SettingRow(
                        icon = "↩",
                        label = "Sign out",
                        tintColor = VibeOnSuccess,
                        onClick = { showSignOutDialog = true },
                        showIconAsText = true,
                    )
                }

                // ABOUT
                SectionLabel("ABOUT")
                SettingsCard {
                    Text(
                        text = "VibeOn",
                        color = VibeOnText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        color = VibeOnTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    RowDivider()
                    SettingRow(
                        icon = "🔄",
                        label = if (checkingUpdate) "Checking…" else "Check for updates",
                        onClick = {
                            if (!checkingUpdate) {
                                viewModel.checkForUpdates { tag ->
                                    updateDialog = UpdateDialog(tag)
                                }
                            }
                        },
                    )
                    RowDivider()
                    Text(
                        text = "Design & Developed by KK\nListen together with friends.",
                        color = VibeOnTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out", color = VibeOnText) },
            text = { Text("Are you sure you want to sign out?", color = VibeOnTextMuted) },
            containerColor = VibeOnBgCard,
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut()
                }) {
                    Text("Sign out", color = VibeOnSuccess, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = VibeOnTextMuted)
                }
            },
        )
    }

    updateDialog?.let { dialog ->
        val latest = dialog.tag
        AlertDialog(
            onDismissRequest = { updateDialog = null },
            title = { Text("Check for updates", color = VibeOnText) },
            text = {
                val current = "v${BuildConfig.VERSION_NAME}"
                when {
                    latest == null -> Text("Could not check for updates. You're on $current.", color = VibeOnTextMuted)
                    isNewerVersion(latest, current) -> Text("Update available · $latest. You have $current.", color = VibeOnTextMuted)
                    else -> Text("You're on the latest version ($current).", color = VibeOnTextMuted)
                }
            },
            containerColor = VibeOnBgCard,
            confirmButton = {
                when {
                    latest != null && isNewerVersion(latest, "v${BuildConfig.VERSION_NAME}") -> {
                        TextButton(onClick = {
                            updateDialog = null
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_LINK))
                            runCatching { context.startActivity(intent) }
                        }) {
                            Text("Download", color = VibeOnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        TextButton(onClick = { updateDialog = null }) {
                            Text("OK", color = VibeOnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDialog = null }) {
                    Text("Cancel", color = VibeOnTextMuted)
                }
            },
        )
    }
}

private data class UpdateDialog(val tag: String?)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = VibeOnTextMuted,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibeOnBgCard, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: String,
    label: String,
    onClick: () -> Unit,
    tintColor: Color = VibeOnPrimary,
    showIconAsText: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIconAsText) {
            Text(text = icon, color = tintColor, fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold)
        } else {
            Text(text = icon, fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = VibeOnText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RowDivider() {
    Spacer(Modifier.height(2.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(VibeOnBorder),
    )
}

private fun initials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .map { it.first().uppercase() }
        .take(2)
        .joinToString("")

private fun AudioQuality.label(): String = when (this) {
    AudioQuality.LOW -> "Low"
    AudioQuality.NORMAL -> "Normal"
    AudioQuality.HIGH -> "High"
}

private fun isNewerVersion(latest: String, current: String): Boolean {
    fun parse(v: String): List<Int> =
        v.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    val l = parse(latest)
    val c = parse(current)
    for (i in 0 until maxOf(l.size, c.size)) {
        val a = l.getOrElse(i) { 0 }
        val b = c.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}