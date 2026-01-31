package com.example.acusen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Hlavní obrazovka aplikace s navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        TabInfo("Monitoring", Icons.Default.PlayArrow),
        TabInfo("Vzory", Icons.Default.List),
        TabInfo("Nahrání", Icons.Default.Add),
        TabInfo("Export/Import", Icons.Default.ImportExport),
        TabInfo("Nastavení", Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Acoustic Sentinel",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> MonitoringScreen()
                1 -> PatternsListScreen()
                2 -> RecordingScreen()
                3 -> ExportImportScreen()
                4 -> SettingsScreen()
            }
        }
    }
}

private data class TabInfo(
    val title: String,
    val icon: ImageVector
)
