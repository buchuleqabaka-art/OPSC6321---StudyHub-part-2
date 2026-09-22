package com.studyhub.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.data.model.Task
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.ui.components.EditProfileDialog
import com.studyhub.app.ui.components.SectionLabel
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.ui.theme.StudyHubTheme
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState
import com.studyhub.app.viewmodel.SettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun ProfileScreen(
    state: MainUiState,
    settings: SettingsUiState,
    onDarkModeChange: (Boolean) -> Unit,
    onOfflineSyncChange: (Boolean) -> Unit,
    onDataSaverChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSaveProfile: (name: String, course: String, email: String) -> Unit,
    onPhotoSelected: (base64: String) -> Unit,
    onSignOut: () -> Unit
) {
    val profile = state.profile
    var showEditProfile by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showComingSoon by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploadingPhoto by remember { mutableStateOf(false) }

    // Android's built-in Photo Picker - needs no runtime permission on any
    // supported API level, and no extra library beyond what activity-compose
    // already brings in.
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadingPhoto = true
            scope.launch(Dispatchers.IO) {
                val base64 = uriToCompressedBase64(context, uri)
                withContext(Dispatchers.Main) {
                    uploadingPhoto = false
                    if (base64 != null) onPhotoSelected(base64)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                photoBase64 = profile?.photoUrl,
                uploading = uploadingPhoto,
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    text = profile?.name ?: "StudyHub Student",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink
                )
                Text(
                    profile?.email ?: "",
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(LilacPrimary.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        "${profile?.yearOfStudy ?: "3rd Year"}. ${profile?.course ?: "Information Technology"}",
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = DateUtils.formatMonthYear(System.currentTimeMillis()),
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBlock(Icons.Default.Groups, state.groups.size.toString(), "Groups")
            StatBlock(Icons.Default.TaskAlt, state.completedTasks.size.toString(), "Task Completed")
            StatBlock(Icons.Default.EmojiEvents, (profile?.points ?: 0).toString(), "Points")
        }

        SectionLabel("Account")

        SettingsGroup {
            SettingRow(
                Icons.Default.Person,
                "Student Profile",
                "View & edit personal information",
                onClick = { showEditProfile = true }
            )
            SettingRow(
                Icons.Default.Language,
                "Language",
                settings.language,
                onClick = { showLanguagePicker = true }
            )
            SettingRow(
                Icons.Default.Security,
                "Privacy & Security",
                "Manage your privacy and account security",
                onClick = { showComingSoon = true }
            )
        }

        SectionLabel("App Preferences")

        SettingsGroup {
            ToggleRow(Icons.Default.DarkMode, "Appearance", "Dark mode", settings.darkMode, onDarkModeChange)
            ToggleRow(Icons.Default.CloudSync, "Offline Settings", "Manage offline data and sync", settings.offlineSync, onOfflineSyncChange)
            ToggleRow(Icons.Default.DataUsage, "Data Usage", "Manage data usage preferences", settings.dataSaver, onDataSaverChange)
            ToggleRow(Icons.Default.Notifications, "Notifications", "Deadline and session reminders", settings.notifications, onNotificationsChange)
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 30.dp)
                .clickable { onSignOut() },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(8.dp))
            Text("Sign Out", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(40.dp))
    }

    if (showEditProfile) {
        EditProfileDialog(
            currentName = profile?.name.orEmpty(),
            currentCourse = profile?.course.orEmpty(),
            currentEmail = profile?.email.orEmpty(),
            onDismiss = { showEditProfile = false },
            onSave = onSaveProfile
        )
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            current = settings.language,
            onDismiss = { showLanguagePicker = false },
            onSelect = onLanguageChange
        )
    }

    if (showComingSoon) {
        ComingSoonDialog(title = "Privacy & Security", onDismiss = { showComingSoon = false })
    }
}

/**
 * Reads the picked image, downsizes it (max 256px on the longest side) and
 * compresses it as JPEG, shrinking the quality further if it's still too big,
 * so the result comfortably fits Firestore's 1 MiB document limit. Returns a
 * plain Base64 string, stored directly in the user's photoUrl field - no
 * Firebase Storage bucket or extra SDK required.
 */
private fun uriToCompressedBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null

        val maxDim = 256
        val scale = maxDim.toFloat() / maxOf(original.width, original.height)
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else original

        var quality = 70
        var bytes: ByteArray
        do {
            val output = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
            bytes = output.toByteArray()
            quality -= 15
        } while (bytes.size > 400_000 && quality > 10)

        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ProfileAvatar(
    photoBase64: String?,
    uploading: Boolean,
    onClick: () -> Unit
) {
    val bitmap = remember(photoBase64) {
        if (photoBase64.isNullOrBlank()) null
        else try {
            val bytes = Base64.decode(photoBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            null // e.g. photoUrl holds a real http(s) URL, not our base64 data
        }
    }

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(LilacPrimary.copy(alpha = 0.25f))
            .clickable(enabled = !uploading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = LilacPrimary,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(26.dp)
                .clip(CircleShape)
                .background(LilacPrimary)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = "Change photo",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun LanguagePickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf("English", "Afrikaans", "isiZulu", "Sesotho", "isiXhosa")
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Choose language", color = Ink) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option,
                            color = Ink,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (option == current) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = LilacPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } }
    )
}

@Composable
private fun ComingSoonDialog(title: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = Ink) },
        text = { Text("This section isn't available in this version yet.", color = InkMuted) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = LilacPrimary) } }
    )
}

@Composable
private fun StatBlock(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(32.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text(label, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(vertical = 8.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = InkMuted)
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LilacPrimary
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE3C6E8)
@Composable
private fun ProfileScreenPreview() {
    StudyHubTheme {
        ProfileScreen(
            state = MainUiState(
                profile = UserProfile(
                    name = "Confidence Silinda",
                    email = "confidencesilinda@gmail.com",
                    yearOfStudy = "3rd Year",
                    course = "Information Technology",
                    points = 100
                ),
                groups = listOf(StudyGroup(), StudyGroup(), StudyGroup(), StudyGroup()),
                tasks = listOf(Task(completed = true), Task(completed = true))
            ),
            settings = SettingsUiState(darkMode = false, offlineSync = true, dataSaver = false, notifications = true),
            onDarkModeChange = {},
            onOfflineSyncChange = {},
            onDataSaverChange = {},
            onNotificationsChange = {},
            onLanguageChange = {},
            onSaveProfile = { _, _, _ -> },
            onPhotoSelected = {},
            onSignOut = {}
        )
    }
}