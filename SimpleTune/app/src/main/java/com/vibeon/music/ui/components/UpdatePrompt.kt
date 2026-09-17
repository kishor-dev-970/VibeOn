package com.vibeon.music.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.vibeon.music.BuildConfig
import com.vibeon.music.data.network.SocialApiClient
import com.vibeon.music.ui.theme.VibeOnBgCard
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted
import com.vibeon.music.util.GITHUB_RELEASES_URL
import com.vibeon.music.util.isNewerVersion

/**
 * Checks the GitHub latest release tag once per app opening and, when a newer version than the
 * running build exists, shows an update prompt that links to the release page.
 */
@Composable
fun UpdatePromptHost(api: SocialApiClient) {
    val context = LocalContext.current
    var latestTag by remember { mutableStateOf<String?>(null) }
    var promptVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val tag = api.latestReleaseTag()
        val current = "v${BuildConfig.VERSION_NAME}"
        if (tag != null && isNewerVersion(tag, current)) {
            latestTag = tag
            promptVisible = true
        }
    }

    if (promptVisible) {
        latestTag?.let { tag ->
            AlertDialog(
                onDismissRequest = { promptVisible = false },
                title = { Text("Update available", color = VibeOnText) },
                text = {
                    Text(
                        "VibeOn ${tag.trimStart('v')} is available. You're on ${BuildConfig.VERSION_NAME}.",
                        color = VibeOnTextMuted,
                    )
                },
                containerColor = VibeOnBgCard,
                confirmButton = {
                    TextButton(onClick = {
                        promptVisible = false
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URL)))
                        }
                    }) {
                        Text("Update", color = VibeOnPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { promptVisible = false }) {
                        Text("Later", color = VibeOnTextMuted)
                    }
                },
            )
        }
    }
}