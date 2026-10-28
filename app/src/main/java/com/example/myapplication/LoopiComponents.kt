package com.example.myapplication

import android.app.Activity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
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

/** Rounded progress bar without Material's end-of-track dot, used for every progress row. */
@Composable
fun LoopiProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = LoopiColors.Blue,
    trackColor: Color = LoopiColors.Track
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress().coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun LoopiBackIcon(tint: Color = LoopiColors.Navy) {
    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tint)
}

@Composable
fun LoopiChevron(tint: Color = LoopiColors.Muted) {
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = tint)
}

/** "1 card", "3 cards". */
fun countLabel(count: Int, noun: String): String = if (count == 1) "1 $noun" else "$count ${noun}s"

private var activeHeroes = 0

/** Lets a child ignore its parent's horizontal padding, so headers can run edge to edge. */
fun Modifier.fullBleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

/**
 * Gradient header used at the top of the main tabs. The gradient covers [height];
 * content can run past it, so a summary card overlaps the bottom edge.
 */
@Composable
fun LoopiHero(
    horizontalPadding: Dp = 18.dp,
    height: Dp = 220.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    // White status bar icons while the dark gradient sits under them.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        // Counted, because the next tab's hero appears before the previous one is disposed.
        activeHeroes++
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            activeHeroes--
            if (activeHeroes == 0) controller?.isAppearanceLightStatusBars = true
        }
    }
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fullBleed(horizontalPadding)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height + statusBar)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(LoopiColors.Navy, LoopiColors.Blue, Color(0xFF5B8DEF))))
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-50).dp)
                    .size(190.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = 30.dp)
                    .size(130.dp)
                    .background(Color.White.copy(alpha = 0.07f), CircleShape)
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(top = statusBar + 14.dp),
            content = content
        )
    }
}

/** Coloured shortcut tile used in the Home quick actions row. */
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f))),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LoopiColors.Navy, maxLines = 1)
        }
    }
}
