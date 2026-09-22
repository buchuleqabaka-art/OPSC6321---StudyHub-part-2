package com.studyhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.ui.components.ScreenHeader
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState
import com.studyhub.app.ui.theme.StudyHubTheme

@Composable
fun SessionsScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onSchedule: () -> Unit,
    onToggleAttendance: (String) -> Unit
) {
    val next = state.nextSession
    val upcoming = state.sessions.filter { it.id != next?.id && it.startTime >= System.currentTimeMillis() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(title = "Study Session", onBack = onBack)

        if (next != null) {
            NextSessionCard(next)
        }

        Text(
            "All Upcoming Sessions",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
            modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp)
        )

        if (upcoming.isEmpty()) {
            EmptyState("No other sessions scheduled yet.")
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(upcoming, key = { it.id }) { session ->
                    SessionRow(session) { onToggleAttendance(session.id) }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(LilacPrimary)
                .clickable { onSchedule() }
                .padding(vertical = 18.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            Spacer(Modifier.size(14.dp))
            Column {
                Text("Schedule a Study Session", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Plan a new study session with your group", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NextSessionCard(session: StudySession) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(LilacPrimary.copy(alpha = 0.45f))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Ink, modifier = Modifier.size(42.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text("Next Session", color = LilacPrimary, fontWeight = FontWeight.Bold)
                Text(session.title, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(session.description, color = Ink, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconLabel(Icons.Default.CalendarMonth, DateUtils.formatDate(session.startTime))
            IconLabel(Icons.Default.Schedule, DateUtils.formatRange(session.startTime, session.endTime))
            IconLabel(Icons.Default.LocationOn, session.location.ifBlank { "TBC" })
        }
    }
}

@Composable
private fun IconLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(4.dp))
        Text(text, color = Ink, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SessionRow(session: StudySession, onToggleAttendance: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleAttendance() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Ink, modifier = Modifier.size(34.dp))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(session.title, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(session.description, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("${session.goingCount} going", color = Ink, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(DateUtils.formatDate(session.startTime).take(6), color = Ink, fontWeight = FontWeight.Bold)
            Text(DateUtils.formatShortDay(session.startTime), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
            Text(DateUtils.formatRange(session.startTime, session.endTime), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFE3C6E8)
@Composable
private fun SessionsScreenPreview() {
    StudyHubTheme {
        SessionsScreen(
            state = MainUiState(
                sessions = listOf(
                    StudySession(
                        title = "Database Normalization",
                        description = "Let's review and practice normalization concepts",
                        location = "Library, Floor 1",
                        startTime = System.currentTimeMillis() + 3_600_000L,
                        endTime = System.currentTimeMillis() + 10_800_000L,
                        attendeeIds = listOf("a", "b", "c")
                    ),
                    StudySession(
                        title = "UI/UX Project Discussion",
                        description = "Discuss project progress and next steps",
                        startTime = System.currentTimeMillis() + 259_200_000L,
                        endTime = System.currentTimeMillis() + 266_400_000L,
                        attendeeIds = listOf("a", "b", "c", "d", "e")
                    )
                )
            ),
            onBack = {},
            onSchedule = {},
            onToggleAttendance = {}
        )
    }
}