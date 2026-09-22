package com.studyhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studyhub.app.ui.components.ScreenHeader
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState
import java.util.Calendar
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.ui.theme.StudyHubTheme


@Composable
fun CalendarScreen(
    state: MainUiState,
    onBack: () -> Unit
) {
    val today = remember { Calendar.getInstance() }
    var month by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var year by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableIntStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    val daysInMonth = DateUtils.daysInMonth(year, month)
    val firstWeekday = DateUtils.firstWeekdayOfMonth(year, month)

    // Millis for the currently selected calendar cell
    val selectedMillis = remember(year, month, selectedDay) {
        Calendar.getInstance().apply {
            set(year, month, selectedDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val dayTasks = state.tasks.filter { DateUtils.isSameDay(it.dueDate, selectedMillis) }
    val daySessions = state.sessions.filter { DateUtils.isSameDay(it.startTime, selectedMillis) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Calendar", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("<", style = MaterialTheme.typography.headlineMedium, color = Ink,
                modifier = Modifier.clickable {
                    if (month == 0) { month = 11; year -= 1 } else month -= 1
                })
            Text(
                text = monthName(month) + " " + year,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )
            Text(">", style = MaterialTheme.typography.headlineMedium, color = Ink,
                modifier = Modifier.clickable {
                    if (month == 11) { month = 0; year += 1 } else month += 1
                })
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, Ink, RoundedCornerShape(10.dp))
                .padding(6.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tues", "Wed", "Thur", "Fri", "Sat").forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Build the grid week by week, padding the first row with blanks
            var day = 1
            val totalCells = firstWeekday + daysInMonth
            val rows = (totalCells + 6) / 7
            repeat(rows) { rowIndex ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val cellIndex = rowIndex * 7 + col
                        if (cellIndex < firstWeekday || day > daysInMonth) {
                            Box(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val thisDay = day
                            val isSelected = thisDay == selectedDay
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) LilacPrimary else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                    .clickable { selectedDay = thisDay },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = thisDay.toString(),
                                    color = Ink,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            day++
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = DateUtils.formatDate(selectedMillis),
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (dayTasks.isEmpty() && daySessions.isEmpty()) {
            Text(
                "Nothing scheduled for this day.",
                color = InkMuted,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        dayTasks.forEach { task ->
            AgendaRow(task.title, DateUtils.formatTime(task.dueDate))
        }
        daySessions.forEach { session ->
            AgendaRow(session.title, DateUtils.formatTime(session.startTime))
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AgendaRow(title: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LilacPrimary.copy(alpha = 0.45f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Ink, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(time, color = Ink)
    }
}

private fun monthName(monthZeroBased: Int): String = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)[monthZeroBased]

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFE3C6E8)
@Composable
private fun CalendarScreenPreview() {
    StudyHubTheme {
        CalendarScreen(
            state = MainUiState(
                tasks = listOf(Task(title = "Database Assignment", dueDate = System.currentTimeMillis())),
                sessions = listOf(StudySession(title = "Study Session", startTime = System.currentTimeMillis() + 3_600_000L, endTime = System.currentTimeMillis() + 10_800_000L))
            ),
            onBack = {}
        )
    }
}