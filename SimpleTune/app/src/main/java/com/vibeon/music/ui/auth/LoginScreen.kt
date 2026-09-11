package com.vibeon.music.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.vibeon.music.ui.components.GradientBackground
import com.vibeon.music.ui.theme.VibeOnBorderLight
import com.vibeon.music.ui.theme.VibeOnError
import com.vibeon.music.ui.theme.VibeOnPrimary
import com.vibeon.music.ui.theme.VibeOnPrimaryLight
import com.vibeon.music.ui.theme.VibeOnText
import com.vibeon.music.ui.theme.VibeOnTextMuted
import com.vibeon.music.ui.theme.VibeOnTextSubtle

@Composable
fun LoginScreen(
    busy: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onSignIn: (firstName: String, lastName: String) -> Unit,
) {
    GradientBackground {
        var firstName by rememberSaveable { mutableStateOf("") }
        var lastName by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(64.dp))
            Box(
                modifier = Modifier
                    .background(VibeOnPrimary, RoundedCornerShape(28.dp))
                    .padding(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.height(44.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "VibeOn",
                color = VibeOnText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Enter your name to join.\nEveryone in the app can see what you're playing.",
                color = VibeOnTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it; if (error != null) onClearError() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("First name", color = VibeOnTextSubtle) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = VibeOnText),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibeOnPrimaryLight,
                    unfocusedBorderColor = VibeOnBorderLight,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = VibeOnPrimary,
                ),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it; if (error != null) onClearError() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Last name", color = VibeOnTextSubtle) },
                singleLine = true,
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { onSignIn(firstName, lastName) }
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Words
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = VibeOnText),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibeOnPrimaryLight,
                    unfocusedBorderColor = VibeOnBorderLight,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = VibeOnPrimary,
                ),
            )

            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = error,
                    color = VibeOnError,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))
            if (busy) {
                CircularProgressIndicator(
                    color = VibeOnPrimary,
                    strokeWidth = 3.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VibeOnPrimary, RoundedCornerShape(16.dp))
                        .clickable { onSignIn(firstName, lastName) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Enter",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "v${com.vibeon.music.BuildConfig.VERSION_NAME}",
                color = VibeOnTextSubtle,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}