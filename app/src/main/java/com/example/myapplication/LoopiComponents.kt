package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.myapplication.ui.theme.LoopiColors

/* =========================================================
   SHARED LOOPI UI
   Pieces reused across flows so every screen looks the same.
   ========================================================= */

private data class LoopiTab(val route: String, val label: String, val icon: ImageVector)

private val loopiTabs = listOf(
    LoopiTab("home", "Home", Icons.Rounded.Home),
    LoopiTab("topics", "Topics", Icons.AutoMirrored.Rounded.MenuBook),
    LoopiTab("review", "Study", Icons.Rounded.Style),
    LoopiTab("progress", "Progress", Icons.Rounded.BarChart),
    LoopiTab("profile", "Profile", Icons.Rounded.Person)
)

@Composable
fun LoopiBottomNavigation(
    navController: NavHostController,
    selected: String
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
        loopiTabs.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab.route,
                onClick = {
                    if (selected != tab.route) {
                        navController.navigate(tab.route) {
                            // Keep one copy of each tab instead of stacking screens on every tap.
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = {
                    Text(
                        tab.label,
                        fontWeight = if (selected == tab.route) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LoopiColors.Blue,
                    selectedTextColor = LoopiColors.Blue,
                    indicatorColor = LoopiColors.BlueSoft,
                    unselectedIconColor = LoopiColors.Muted,
                    unselectedTextColor = LoopiColors.Muted
                )
            )
        }
    }
}

/** Picks an icon from the topic's title first, then its category. */
fun topicIcon(title: String, category: String): ImageVector {
    val text = "$title $category".lowercase()
    return when {
        "database" in text || "sql" in text -> Icons.Rounded.Storage
        "network" in text -> Icons.Rounded.Wifi
        "algorithm" in text || "data structure" in text -> Icons.Rounded.AccountTree
        "operating" in text || "system" in text -> Icons.Rounded.Memory
        "security" in text || "cyber" in text -> Icons.Rounded.Security
        "ai" in text.split(' ') || "machine" in text || "intelligence" in text -> Icons.Rounded.Psychology
        "programming" in text || "software" in text || "code" in text -> Icons.Rounded.Code
        "math" in text || "calculus" in text || "statistics" in text -> Icons.Rounded.Functions
        "business" in text || "economics" in text -> Icons.Rounded.Business
        "engineering" in text -> Icons.Rounded.Settings
        "science" in text || "biology" in text || "chemistry" in text -> Icons.Rounded.Biotech
        "english" in text || "language" in text -> Icons.Rounded.Language
        "information technology" in text -> Icons.Rounded.Memory
        else -> Icons.Rounded.School
    }
}

private val topicAccents = listOf(
    LoopiColors.Blue,
    LoopiColors.Teal,
    LoopiColors.Purple,
    LoopiColors.Amber,
    LoopiColors.Pink,
    Color(0xFF0EA5C6)
)

/** Stable accent per topic so the same topic keeps its colour on every screen. */
fun topicAccent(title: String): Color =
    topicAccents[(title.trim().lowercase().hashCode() and Int.MAX_VALUE) % topicAccents.size]

@Composable
fun TopicIconTile(
    title: String,
    category: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val accent = topicAccent(title)
    Box(
        modifier = modifier
            .size(size)
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = topicIcon(title, category),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}
