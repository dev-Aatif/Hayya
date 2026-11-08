package dev.hayya.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top-level application destinations accessible via the modern floating navbar.
 */
enum class AppNavTab(val label: String) {
    DASHBOARD("Prayers"),
    TOOLS("Companion"),
    SETTINGS("Settings")
}

/**
 * Modern floating pill navigation bar docked elegantly above the bottom system insets.
 * Features Material 3 color harmony, tactile feedback, and adaptive indicator expansion.
 */
@Composable
fun FloatingNavBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(60.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color(0x66000000),
                    ambientColor = Color(0x33000000)
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple(AppNavTab.DASHBOARD, "Prayers", Icons.Outlined.AccessTime to Icons.Filled.AccessTime),
                    Triple(AppNavTab.TOOLS, "Companion", Icons.Outlined.Explore to Icons.Filled.Explore),
                    Triple(AppNavTab.SETTINGS, "Settings", Icons.Outlined.Settings to Icons.Filled.Settings)
                )

                tabs.forEach { (tab, title, icons) ->
                    val selected = currentTab == tab
                    val (unselectedIcon, selectedIcon) = icons

                    val pillBgColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pill_bg_color"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pill_content_color"
                    )

                    val pillPaddingHorizontal by animateDpAsState(
                        targetValue = if (selected) 18.dp else 14.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pill_padding"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(pillBgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(horizontal = pillPaddingHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) selectedIcon else unselectedIcon,
                                contentDescription = title,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            if (selected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp,
                                        color = contentColor
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
