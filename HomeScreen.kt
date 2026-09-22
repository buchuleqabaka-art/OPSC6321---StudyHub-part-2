package com.studyhub.app.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studyhub.app.data.model.QuoteDto
import com.studyhub.app.data.model.Task
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.ui.theme.StudyHubTheme
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState


@Composable
fun HomeScreen(
    state: MainUiState,
    notificationsEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    onOpenGroups: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSessions: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    var showNotificationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Ink,
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onOpenDrawer() }
            )
            Spacer(Modifier.weight(1f))
            Icon(
                if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = "Notifications",
                tint = if (notificationsEnabled) Ink else InkMuted,
                modifier = Modifier
                    .size(30.dp)
                    .clickable { showNotificationDialog = true }
            )
            Spacer(Modifier.size(14.dp))

            val photoUrl = state.profile?.photoUrl
            val profileBitmap = remember(photoUrl) {
                if (photoUrl.isNullOrBlank()) null
                else try {
                    val bytes = Base64.decode(photoUrl, Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }

            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onOpenProfile() }
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Ink,
                    modifier = Modifier
                        .size(34.dp)
                        .clickable { onOpenProfile() }
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        val firstName = state.profile?.name?.trim()?.takeIf { it.isNotBlank() }?.substringBefore(" ") ?: "there"
        Text(
            text = "HI, ${firstName.uppercase()}!!",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink
        )
        Text("Let's make today productive.", color = Ink)

        // Motivational line pulled from our REST API
        state.quote?.let { quote ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "\"${quote.content}\" - ${quote.author}",
                color = InkMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(22.dp))

        DeadlinesCard(tasks = state.upcomingTasks, onViewAll = onOpenTasks)

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickTile("My Groups", Icons.Default.Groups, Modifier.weight(1f), onOpenGroups)
            QuickTile("Tasks", Icons.AutoMirrored.Filled.Assignment, Modifier.weight(1f), onOpenTasks)
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickTile("Calendar", Icons.Default.CalendarMonth, Modifier.weight(1f), onOpenCalendar)
            QuickTile("Study Sessions", Icons.Default.Schedule, Modifier.weight(1f), onOpenSessions)
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            icon = {
                Icon(
                    if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = LilacPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Notifications & Reminders",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable Notifications",
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                if (notificationsEnabled) "Reminders are active" else "Reminders are paused",
                                color = InkMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { onToggleNotifications(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        "Upcoming Reminders",
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    if (!notificationsEnabled) {
                        Text(
                            "Notifications are currently paused. Enable notifications above to receive deadline and session reminders.",
                            color = InkMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else if (state.upcomingTasks.isEmpty() && state.nextSession == null) {
                        Text(
                            "No upcoming deadlines or study sessions right now.",
                            color = InkMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        state.upcomingTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = LilacPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        task.title,
                                        color = Ink,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Due: ${DateUtils.dueInLabel(task.dueDate)}",
                                        color = InkMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        state.nextSession?.let { session ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = LilacPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        session.title,
                                        color = Ink,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Session: ${DateUtils.formatDate(session.startTime)}",
                                        color = InkMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Close", color = LilacPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DeadlinesCard(tasks: List<Task>, onViewAll: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Upcoming Deadlines",
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "View all",
                color = LilacPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Text("Nothing due right now. Add a task to get started.", color = InkMuted)
        } else {
            tasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onViewAll() }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LilacPrimary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = Ink)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(task.title, color = Ink, fontWeight = FontWeight.Bold)
                        Text(
                            DateUtils.dueInLabel(task.dueDate),
                            color = InkMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .clickable { onClick() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Ink, modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, color = Ink, fontWeight = FontWeight.Bold)
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFE3C6E8
)
@Composable
private fun HomeScreenPreview() {
    StudyHubTheme {
        HomeScreen(
            state = MainUiState(
                profile = UserProfile(name = "Confidence Silinda"),
                quote = QuoteDto(
                    content = "Small consistent effort beats one long night before the deadline.",
                    author = "StudyHub"
                ),
                tasks = listOf(
                    Task(
                        title = "Database Assignment",
                        dueDate = System.currentTimeMillis() + 172_800_000L
                    ),
                    Task(
                        title = "UI/UX Project",
                        dueDate = System.currentTimeMillis() + 432_000_000L
                    )
                )
            ),
            onOpenGroups = {},
            onOpenTasks = {},
            onOpenCalendar = {},
            onOpenSessions = {},
            onOpenProfile = {},
            onOpenDrawer = {}
        )
    }
}
