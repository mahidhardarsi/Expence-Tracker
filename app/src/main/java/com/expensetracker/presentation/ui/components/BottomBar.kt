package com.expensetracker.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.vector.ImageVector
import com.expensetracker.presentation.ui.AppTab
import com.expensetracker.presentation.ui.theme.appColors

private data class TabItem(
    val tab: AppTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

private val tabs = listOf(
    TabItem(AppTab.Reports, Icons.Filled.Description, Icons.Outlined.Description, "Reports"),
    TabItem(AppTab.Analysis, Icons.Filled.BarChart, Icons.Outlined.BarChart, "Analysis"),
    TabItem(AppTab.Options, Icons.Filled.Settings, Icons.Outlined.Settings, "Options")
)

@Composable
fun AppBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        modifier = androidx.compose.ui.Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.appColors.surface,
        contentColor = MaterialTheme.appColors.primaryText,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        tabs.forEach { item ->
            val selected = currentTab.route == item.tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.appColors.accentBlue,
                    selectedTextColor = MaterialTheme.appColors.accentBlue,
                    indicatorColor = MaterialTheme.appColors.accentBlue.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.appColors.mutedText,
                    unselectedTextColor = MaterialTheme.appColors.mutedText
                )
            )
        }
    }
}
