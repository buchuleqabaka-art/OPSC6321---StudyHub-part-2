package com.studyhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.ui.components.FilterChipRow
import com.studyhub.app.ui.components.ScreenHeader
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.viewmodel.MainUiState
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.ui.theme.StudyHubTheme

@Composable
fun GroupsScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onCreateGroup: () -> Unit,
    onOpenGroup: (StudyGroup) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val myUid = state.profile?.uid.orEmpty()

    // Search + All/Joined/Owned filtering happens locally on the live list
    val visible = state.groups.filter { group ->
        val matchesQuery = query.isBlank() ||
                group.name.contains(query, ignoreCase = true) ||
                group.subject.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            "Owned" -> group.ownerId == myUid
            "Joined" -> group.ownerId != myUid
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = "My Groups",
            onBack = onBack,
            trailing = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create group",
                    tint = LilacPrimary,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { onCreateGroup() }
                )
            }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            placeholder = { Text("Search groups", color = InkMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink) },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.55f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.55f),
                focusedIndicatorColor = Ink,
                unfocusedIndicatorColor = Ink
            )
        )

        Spacer(Modifier.height(16.dp))

        FilterChipRow(
            options = listOf("All", "Joined", "Owned"),
            selected = filter,
            onSelect = { filter = it }
        )

        Spacer(Modifier.height(18.dp))

        if (visible.isEmpty()) {
            EmptyState("No groups yet. Tap + to create your first study group.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp)
            ) {
                items(visible, key = { it.id }) { group ->
                    GroupRow(group) { onOpenGroup(group) }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: StudyGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LilacPrimary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = Ink)
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(group.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("${group.memberCount} members", color = InkMuted)
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = InkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFE3C6E8
)
@Composable
private fun GroupsScreenPreview() {
    StudyHubTheme {
        GroupsScreen(
            state = MainUiState(
                profile = UserProfile(uid = "me", name = "Confidence Silinda"),
                groups = listOf(
                    StudyGroup(
                        name = "Database Systems",
                        memberIds = listOf("a", "b", "c", "d", "e"),
                        ownerId = "someoneElse"
                    ),
                    StudyGroup(
                        name = "Mobile App Development",
                        memberIds = listOf("a", "b", "c", "d", "e", "f"),
                        ownerId = "me"
                    ),
                    StudyGroup(
                        name = "Web Development",
                        memberIds = List(9) { "u$it" },
                        ownerId = "someoneElse"
                    )
                )
            ),
            onBack = {},
            onCreateGroup = {},
            onOpenGroup = {}
        )
    }
}