package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp

enum class DrishtiTab {
    HOME, ASSISTANT, LOG, PROFILE
}

@Composable
fun DrishtiNavBar(
    selectedTab: DrishtiTab,
    onTabSelected: (DrishtiTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    data class TabInfo(
        val tab: DrishtiTab,
        val label: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
    )

    val tabs = listOf(
        TabInfo(DrishtiTab.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        TabInfo(DrishtiTab.ASSISTANT, "Assistant", Icons.Filled.Mic, Icons.Outlined.Mic),
        TabInfo(DrishtiTab.LOG, "Log", Icons.Filled.Assignment, Icons.Outlined.Assignment),
        TabInfo(DrishtiTab.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
    )

    NavigationBar(
        modifier = modifier,
        containerColor = AppColor.Surface,
    ) {
        tabs.forEach { info ->
            val selected = selectedTab == info.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(info.tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) info.selectedIcon else info.unselectedIcon,
                        contentDescription = info.label,
                        tint = if (selected) AppColor.PrimaryAccent else AppColor.Secondary,
                    )
                },
                label = {
                    Text(
                        text = info.label,
                        fontSize = 11.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AppColor.PrimaryAccent.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
