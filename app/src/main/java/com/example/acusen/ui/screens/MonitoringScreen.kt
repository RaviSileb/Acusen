package com.example.acusen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.acusen.viewmodel.MonitoringViewModel

/**
 * Obrazovka pro monitoring a kontrolu služby
 */
@Composable
fun MonitoringScreen() {
    val viewModel: MonitoringViewModel = viewModel()
    val scrollState = rememberScrollState()

    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val serviceStatus by viewModel.serviceStatus.collectAsState()
    val lastDetection by viewModel.lastDetection.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val historyStatistics by viewModel.historyStatistics.collectAsState()
    val activePatterns by viewModel.activePatterns.collectAsState()
    val detectedAlarms by viewModel.detectedAlarms.collectAsState()
    val monitoringDuration by viewModel.monitoringDuration.collectAsState()

    // Aktualizace statistik při spuštění
    LaunchedEffect(Unit) {
        viewModel.refreshHistoryStatistics()
    }

    // Pravidelná aktualizace doby monitoringu
    LaunchedEffect(isMonitoring) {
        if (isMonitoring) {
            while (isMonitoring) {
                viewModel.updateMonitoringDuration()
                kotlinx.coroutines.delay(1000) // Aktualizace každou sekundu
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status karty
        MonitoringStatusCard(
            isMonitoring = isMonitoring,
            serviceStatus = serviceStatus,
            onToggleMonitoring = { viewModel.toggleMonitoring() },
            onSimulateDetection = { viewModel.simulateAlarmDetection() }
        )

        // Historie detekovaných alarmů (PRVNÍ)
        AlarmHistoryCard(
            detectedAlarms = detectedAlarms,
            onClearHistory = { viewModel.clearAlarmHistory() },
            onAddTestData = { viewModel.addTestAlarmData() },
            onDeleteAlarm = { alarmId -> viewModel.deleteAlarmById(alarmId) }
        )

        // Poslední detekce
        lastDetection?.let { detection ->
            LastDetectionCard(detection = detection)
        }

        // Statistiky
        StatisticsCard(
            statistics = historyStatistics,
            activePatternCount = activePatterns.size,
            monitoringDuration = monitoringDuration,
            isMonitoring = isMonitoring
        )

        // Oprávnění (DRUHÉ)
        PermissionsCard(
            permissions = permissionStatus
        )

        // Spacer pro lepší scrollování
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MonitoringStatusCard(
    isMonitoring: Boolean,
    serviceStatus: String,
    onToggleMonitoring: () -> Unit,
    onSimulateDetection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.PlayArrow else Icons.Default.Clear,
                    contentDescription = null,
                    tint = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "MONITORING",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Stav: $serviceStatus",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMonitoring) "ZASTAVIT MONITORING" else "SPUSTIT MONITORING"
                )
            }

            // TEST tlačítko pro simulaci detekce (pouze pro testování)
            if (isMonitoring) {
                OutlinedButton(
                    onClick = onSimulateDetection,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🚨 TEST DETEKCE ALARMU", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    permissions: Map<String, Boolean>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Oprávnění aplikace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            permissions.forEach { (permission, granted) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getPermissionDisplayName(permission),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Celkový status oprávnění
            val grantedCount = permissions.count { it.value }
            val totalCount = permissions.size

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Celkový stav:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$grantedCount/$totalCount uděleno",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (grantedCount == totalCount)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LastDetectionCard(
    detection: MonitoringViewModel.DetectionInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "POSLEDNÍ DETEKCE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = "Vzor: ${detection.patternName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "Přesnost: ${(detection.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "Čas: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(detection.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun StatisticsCard(
    statistics: com.example.acusen.storage.AlarmHistoryStats,
    activePatternCount: Int,
    monitoringDuration: Long,
    isMonitoring: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Statistiky monitoringu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Aktivní vzory v detekci:")
                Text(
                    "$activePatternCount",
                    fontWeight = FontWeight.Medium,
                    color = if (activePatternCount > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Detekce dnes:")
                Text(statistics.todayDetections.toString(), fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Detekce celkem:")
                Text(statistics.totalDetections.toString(), fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Úspěšnost detekce:")
                Text("${(statistics.averageConfidence * 100).toInt()}%",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Detekce s GPS:")
                Text("${statistics.detectionsWithGPS}/${statistics.totalDetections}",
                    fontWeight = FontWeight.Medium,
                    color = if (statistics.detectionsWithGPS > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Doba monitoringu:")
                Text(
                    formatDuration(monitoringDuration, isMonitoring),
                    fontWeight = FontWeight.Medium,
                    color = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long, isActive: Boolean): String {
    if (durationMs == 0L && !isActive) {
        return "Neaktivní"
    }

    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60)) % 24

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun AlarmHistoryCard(
    detectedAlarms: List<com.example.acusen.data.AlarmDetection>,
    onClearHistory: () -> Unit,
    onAddTestData: () -> Unit,
    onDeleteAlarm: (String) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historie detekovaných alarmů",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Celkem: ${detectedAlarms.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Tlačítko pro přidání test dat
                    IconButton(
                        onClick = onAddTestData,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Přidat test data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Tlačítko pro vymazání historie
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.size(32.dp),
                        enabled = detectedAlarms.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Vymazat historii",
                            tint = if (detectedAlarms.isNotEmpty())
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Seznam detekovaných alarmů
            if (detectedAlarms.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    detectedAlarms.take(10).forEach { alarm -> // Zobrazí posledních 10
                        AlarmHistoryItem(
                            alarm = alarm,
                            onDelete = { onDeleteAlarm(alarm.id) }
                        )

                        if (alarm != detectedAlarms.take(10).last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }

                    if (detectedAlarms.size > 10) {
                        Text(
                            text = "... a ${detectedAlarms.size - 10} dalších",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Zatím nebyl detekován žádný alarm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Potvrzovací dialog pro vymazání historie
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Vymazat historii") },
            text = {
                Text("Opravdu chcete vymazat celou historii detekovaných alarmů? Tuto akci nelze vrátit zpět.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Vymazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
private fun AlarmHistoryItem(
    alarm: com.example.acusen.data.AlarmDetection,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Název detekovaného zvuku
            Text(
                text = alarm.patternName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Datum a čas
            Text(
                text = formatDateTime(alarm.detectedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // GPS informace
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (alarm.gpsLatitude != null && alarm.gpsLongitude != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = alarm.getShortGpsString() ?: "GPS nedostupná",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Přesnost detekce
            Text(
                text = "${(alarm.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Tlačítko pro smazání
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Smazat alarm",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Potvrzovací dialog pro smazání
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Smazat alarm") },
            text = {
                Text("Opravdu chcete smazat tento záznam detekce alarmu \"${alarm.patternName}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun getPermissionDisplayName(permission: String): String {
    return when {
        permission.contains("RECORD_AUDIO") -> "Mikrofon"
        permission.contains("ACCESS_FINE_LOCATION") -> "Přesná lokace"
        permission.contains("ACCESS_COARSE_LOCATION") -> "Přibližná lokace"
        permission.contains("FOREGROUND_SERVICE") -> "Background služba"
        permission.contains("POST_NOTIFICATIONS") -> "Oznámení"
        else -> permission.substringAfterLast(".")
    }
}

@Composable
private fun ActionButtonsCard(
    permissionCount: Int,
    totalPermissions: Int,
    onShowPermissions: () -> Unit,
    onShowStatistics: () -> Unit,
    onShowAlarmHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Podrobné informace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Tlačítko oprávnění
            OutlinedButton(
                onClick = onShowPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null
                        )
                        Text("Oprávnění aplikace")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "$permissionCount/$totalPermissions",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tlačítko statistik
            OutlinedButton(
                onClick = onShowStatistics,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null
                        )
                        Text("Statistiky monitoringu")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Zobrazit",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tlačítko historie alarmů
            OutlinedButton(
                onClick = onShowAlarmHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null
                        )
                        Text("Historie detekovaných alarmů")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Zobrazit",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
