package com.nuevoso.launcher.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nuevoso.launcher.ui.theme.SolSurface
import com.nuevoso.launcher.ui.theme.SolTextSoft
import com.nuevoso.launcher.ui.theme.SolTerracotta

enum class DockDestination { Home, Apps, Conversation, Settings }

@Composable
fun DockNav(
    currentDestination: DockDestination,
    onDestinationSelected: (DockDestination) -> Unit,
    isOrbActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(SolSurface)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockItem(
            icon = Icons.Default.Home,
            label = "Inicio",
            selected = currentDestination == DockDestination.Home,
            showMiniOrb = currentDestination == DockDestination.Home,
            orbActive = isOrbActive,
            onClick = { onDestinationSelected(DockDestination.Home) },
        )
        DockItem(
            icon = Icons.Default.Apps,
            label = "Apps",
            selected = currentDestination == DockDestination.Apps,
            onClick = { onDestinationSelected(DockDestination.Apps) },
        )
        DockItem(
            icon = Icons.Default.ChatBubbleOutline,
            label = "Conversar",
            selected = currentDestination == DockDestination.Conversation,
            onClick = { onDestinationSelected(DockDestination.Conversation) },
        )
        DockItem(
            icon = Icons.Default.Settings,
            label = "Ajustes",
            selected = currentDestination == DockDestination.Settings,
            onClick = { onDestinationSelected(DockDestination.Settings) },
        )
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showMiniOrb: Boolean = false,
    orbActive: Boolean = false,
) {
    val tint = if (selected) SolTerracotta else SolTextSoft

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (showMiniOrb && selected) {
            SolOrb(
                state = if (orbActive) OrbState.Thinking else OrbState.Idle,
                sizeDp = 28.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
