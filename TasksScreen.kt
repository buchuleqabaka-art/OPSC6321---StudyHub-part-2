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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.studyhub.app.data.model.Priority
import com.studyhub.app.data.model.Task
import com.studyhub.app.ui.components.FilterChipRow
import com.studyhub.app.ui.components.ScreenHeader
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.ui.theme.PriorityHigh
import com.studyhub.app.ui.theme.PriorityLow
import com.studyhub.app.ui.theme.PriorityMedium
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState
import com.studyhub.app.ui.theme.StudyHubTheme

@Composable
fun TasksScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onNewTask: () -> Unit,
    onToggle: (Task) -> Unit,
    onDelete: (String) -> Unit
) {
    var filter by remember { mutableStateOf("All") }

    val visible = when (filter) {
        "Pending" -> state.pendingTasks
        "Completed" -> state.completedTasks
        else -> state.tasks
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(title = "Tasks", onBack = onBack)

        Spacer(Modifier.height(10.dp))

        FilterChipRow(
            options = listOf("All", "Pending", "Completed"),
            selected = filter,
            onSelect = { filter = it }
        )

        Spacer(Modifier.height(18.dp))

        if (visible.isEmpty()) {
            EmptyState("No tasks here yet. Tap New Task to add one.")
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(visible, key = { it.id }) { task ->
                    TaskRow(task, onToggle = { onToggle(task) }, onDelete = { onDelete(task.id) })
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(LilacPrimary)
                .clickable { onNewTask() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.padding(4.dp))
            Text("New Task", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = LilacPrimary)
            )
            Text(
                text = task.title,
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )
        }
        Text("Due: ${DateUtils.formatDate(task.dueDate)}", color = Ink, modifier = Modifier.padding(start = 12.dp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Text(
                "Assigned to: ${task.assignedToName.ifBlank { "You" }}",
                color = Ink
            )
            Spacer(Modifier.weight(1f))
            PriorityChip(task.priorityEnum)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Delete",
            color = InkMuted,
            modifier = Modifier
                .padding(start = 12.dp)
                .clickable { onDelete() }
        )
    }
}

@Composable
private fun PriorityChip(priority: Priority) {
    val (label, colour) = when (priority) {
        Priority.HIGH -> "High" to PriorityHigh
        Priority.MEDIUM -> "Medium" to PriorityMedium
        Priority.LOW -> "Low" to PriorityLow
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colour)
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Text(label, color = Ink, fontWeight = FontWeight.Medium)
    }
}
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFE3C6E8)
@Composable
private fun TasksScreenPreview() {
    StudyHubTheme {
        TasksScreen(
            state = MainUiState(
                tasks = listOf(
                    Task(title = "Database Assignment", dueDate = System.currentTimeMillis() + 172_800_000L, priority = "HIGH", assignedToName = "You"),
                    Task(title = "UI/UX Project", dueDate = System.currentTimeMillis() + 432_000_000L, priority = "MEDIUM", assignedToName = "Bee"),
                    Task(title = "Report Documentation", dueDate = System.currentTimeMillis() + 604_800_000L, priority = "LOW", assignedToName = "Daisy", completed = true)
                )
            ),
            onBack = {},
            onNewTask = {},
            onToggle = {},
            onDelete = {}
        )
    }
}