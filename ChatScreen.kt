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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.studyhub.app.data.model.ChatMessage
import com.studyhub.app.data.model.StudyGroup
import com.studyhub.app.data.model.UserProfile
import com.studyhub.app.ui.components.ScreenHeader
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.ui.theme.StudyHubTheme
import com.studyhub.app.util.DateUtils
import com.studyhub.app.viewmodel.MainUiState

@Composable
fun ChatScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onSelectGroup: (String) -> Unit = {},
    onOpenGroups: () -> Unit = {},
    onSend: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var showGroupPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val myUid = state.profile?.uid.orEmpty()
    val activeGroup = state.groups.firstOrNull { it.id == state.activeGroupId }
    val groupName = activeGroup?.name ?: "Select a group"

    // Keep the newest message in view
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(title = "My Chats", onBack = onBack)

        // Interactive Group Selector Chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier.clickable { showGroupPicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = LilacPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = groupName,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select a group",
                        tint = Ink,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (state.activeGroupId == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = LilacPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No Group Selected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Select a study group to view messages and chat with your peers.",
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showGroupPicker = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LilacPrimary)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Select a Group", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message, isMine = message.senderId == myUid)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", color = InkMuted) },
                shape = RoundedCornerShape(26.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.6f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) { onSend(draft); draft = "" }
                },
                enabled = draft.isNotBlank() && state.activeGroupId != null
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = LilacPrimary)
            }
        }
    }

    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            icon = {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = LilacPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Select Study Group",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.groups.isEmpty()) {
                        Text(
                            text = "You haven't joined or created any study groups yet.",
                            color = InkMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        Text(
                            text = "Choose a group to open its chat room:",
                            color = InkMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                            items(state.groups, key = { it.id }) { group ->
                                val isSelected = group.id == state.activeGroupId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) LilacPrimary.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        )
                                        .clickable {
                                            onSelectGroup(group.id)
                                            showGroupPicker = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(LilacPrimary.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = Ink)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            group.name,
                                            color = Ink,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            "${group.memberCount} members • ${group.subject}",
                                            color = InkMuted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = LilacPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (state.groups.isEmpty()) {
                    Button(
                        onClick = {
                            showGroupPicker = false
                            onOpenGroups()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LilacPrimary)
                    ) {
                        Text("Explore Groups", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { showGroupPicker = false }) {
                        Text("Cancel", color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isMine: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isMine) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = LilacPrimary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.size(6.dp))
                Text(message.senderName, color = Ink, fontWeight = FontWeight.Bold)
            }
            Text(
                text = DateUtils.formatDate(message.sentAt) + " " + DateUtils.formatTime(message.sentAt),
                color = InkMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isMine) LilacPrimary.copy(alpha = 0.7f) else Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(message.text, color = Ink)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE3C6E8)
@Composable
private fun ChatScreenPreview() {
    StudyHubTheme {
        ChatScreen(
            state = MainUiState(
                profile = UserProfile(uid = "me", name = "Confidence Silinda"),
                activeGroupId = "g1",
                groups = listOf(StudyGroup(id = "g1", name = "Database Systems")),
                messages = listOf(
                    ChatMessage(senderId = "bee", senderName = "Bee", text = "Hey guys, dont forget about the ERD task"),
                    ChatMessage(senderId = "me", senderName = "You", text = "Yes, On it!"),
                    ChatMessage(senderId = "daisy", senderName = "Daisy", text = "I've updated the sample schema, i will send it!")
                )
            ),
            onBack = {},
            onSend = {}
        )
    }
}