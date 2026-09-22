package com.studyhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studyhub.app.ui.navigation.Routes
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.LilacPrimary

private data class BarItem(val route: String, val label: String, val icon: ImageVector)

/** Bottom navigation with the raised "+" action in the middle, as designed. */
@Composable
fun StudyHubBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val left = listOf(
        BarItem(Routes.HOME, "Home", Icons.Default.Home),
        BarItem(Routes.GROUPS, "Groups", Icons.Default.Groups)
    )
    val right = listOf(
        BarItem(Routes.CHATS, "Chats", Icons.Default.Chat),
        BarItem(Routes.PROFILE, "Profile", Icons.Default.Person)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        left.forEach { NavItem(it, currentRoute, onNavigate, Modifier.weight(1f)) }

        Box(
            modifier = Modifier
                .weight(1f)
                .size(54.dp)
                .clip(CircleShape)
                .background(LilacPrimary)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White, modifier = Modifier.size(30.dp))
        }

        right.forEach { NavItem(it, currentRoute, onNavigate, Modifier.weight(1f)) }
    }
}

@Composable
private fun NavItem(
    item: BarItem,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = currentRoute == item.route
    Column(
        modifier = modifier.clickable { onNavigate(item.route) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) LilacPrimary else Ink,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) LilacPrimary else Ink,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}