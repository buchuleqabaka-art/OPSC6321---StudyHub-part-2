package com.studyhub.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.studyhub.app.data.model.StudySession
import com.studyhub.app.data.model.Task
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.util.DateUtils
import java.util.Calendar

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, subject: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val valid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New study group", color = Ink) },
        text = {
            Column {
                LabeledField("Group name", name, { name = it }, Icons.Default.Groups, placeholder = "e.g. Database Systems")
                Spacer(Modifier.height(12.dp))
                LabeledField("Subject", subject, { subject = it }, Icons.Default.Subject, placeholder = "e.g. Oracle SQL")
                Spacer(Modifier.height(12.dp))
                LabeledField("Description", description, { description = it }, Icons.Default.Notes, placeholder = "What is this group for?")
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onCreate(name, subject, description); onDismiss() }) {
                Text("Create", color = if (valid) LilacPrimary else Ink)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } }
    )
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (Task) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var dueDate by remember { mutableLongStateOf(System.currentTimeMillis() + 86_400_000L) }
    val valid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New task", color = Ink) },
        text = {
            Column {
                LabeledField("Title", title, { title = it }, Icons.Default.Assignment, placeholder = "e.g. Database Assignment")
                Spacer(Modifier.height(12.dp))
                LabeledField("Assigned to", assignee, { assignee = it }, Icons.Default.Groups, placeholder = "You")
                Spacer(Modifier.height(12.dp))

                Text("Priority", color = Ink)
                Spacer(Modifier.height(6.dp))
                FilterChipRow(
                    options = listOf("HIGH", "MEDIUM", "LOW"),
                    selected = priority,
                    onSelect = { priority = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            dueDate = Calendar.getInstance().apply { set(y, m, d, 9, 0, 0) }.timeInMillis
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text("Due: ${DateUtils.formatDate(dueDate)}", color = LilacPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onCreate(
                    Task(
                        title = title.trim(),
                        assignedToName = assignee.ifBlank { "You" },
                        priority = priority,
                        dueDate = dueDate
                    )
                )
                onDismiss()
            }) { Text("Add task", color = if (valid) LilacPrimary else Ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } }
    )
}

@Composable
fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onCreate: (StudySession) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var start by remember { mutableLongStateOf(System.currentTimeMillis() + 86_400_000L) }
    val valid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Schedule a study session", color = Ink) },
        text = {
            Column {
                LabeledField("Title", title, { title = it }, Icons.Default.CalendarMonth, placeholder = "e.g. Database Normalization")
                Spacer(Modifier.height(12.dp))
                LabeledField("Description", description, { description = it }, Icons.Default.Notes, placeholder = "What will you cover?")
                Spacer(Modifier.height(12.dp))
                LabeledField("Location", location, { location = it }, Icons.Default.LocationOn, placeholder = "e.g. Library, Floor 1")
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = start }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            start = Calendar.getInstance().apply { set(y, m, d, 14, 0, 0) }.timeInMillis
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text("Starts: ${DateUtils.formatDate(start)} 14:00", color = LilacPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onCreate(
                    StudySession(
                        title = title.trim(),
                        description = description.trim(),
                        location = location.trim(),
                        startTime = start,
                        endTime = start + 2 * 60 * 60 * 1000L
                    )
                )
                onDismiss()
            }) { Text("Schedule", color = if (valid) LilacPrimary else Ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } }
    )
}

@Composable
fun EditAccountDialog(
    field: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    val valid = value.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit ${field.replaceFirstChar { it.uppercase() }}", color = Ink) },
        text = {
            Column {
                LabeledField(
                    field.replaceFirstChar { it.uppercase() },
                    value,
                    { value = it },
                    Icons.Default.Assignment,
                    placeholder = "Enter new $field"
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(field, value); onDismiss() }) {
                Text("Save", color = if (valid) LilacPrimary else Ink)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } }
    )
}
