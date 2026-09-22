package com.studyhub.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.util.Validators

/**
 * Lets the student edit their name, course and profile email. Note: this
 * updates the StudyHub profile document only - it does not change the
 * Firebase Authentication sign-in email, which requires a separate
 * re-authentication step Firebase enforces for security.
 */
@Composable
fun EditProfileDialog(
    currentName: String,
    currentCourse: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (name: String, course: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var course by remember { mutableStateOf(currentCourse) }
    var email by remember { mutableStateOf(currentEmail) }

    val valid = Validators.isValidName(name) && Validators.isValidEmail(email)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit profile", color = Ink) },
        text = {
            Column {
                LabeledField(
                    label = "Name",
                    value = name,
                    onValueChange = { name = it },
                    leadingIcon = Icons.Default.Person,
                    placeholder = "Your full name"
                )
                Spacer(Modifier.height(12.dp))
                LabeledField(
                    label = "Course",
                    value = course,
                    onValueChange = { course = it },
                    leadingIcon = Icons.Default.School,
                    placeholder = "e.g. Information Technology"
                )
                Spacer(Modifier.height(12.dp))
                LabeledField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    leadingIcon = Icons.Default.Email,
                    placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "This updates your StudyHub profile. Your sign-in email stays the same.",
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(name.trim(), course.trim(), email.trim())
                    onDismiss()
                }
            ) {
                Text("Save", color = if (valid) LilacPrimary else Ink)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Ink) } }
    )
}