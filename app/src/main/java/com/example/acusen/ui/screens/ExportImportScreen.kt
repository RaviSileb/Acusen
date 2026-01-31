package com.example.acusen.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.acusen.viewmodel.ExportImportViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Obrazovka pro export a import zvukových vzorů
 */
@Composable
fun ExportImportScreen() {
    val viewModel: ExportImportViewModel = viewModel()
    val scrollState = rememberScrollState()

    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val availablePatternsCount by viewModel.availablePatternsCount.collectAsState()

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportPatterns(it) }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.analyzeImportFile(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Úvodní informace
        InfoCard()

        // Export sekce
        ExportCard(
            availablePatternsCount = availablePatternsCount,
            exportState = exportState,
            onExport = {
                val fileName = viewModel.getExportFileName()
                exportLauncher.launch(fileName)
            },
            onResetExport = { viewModel.resetExportState() }
        )

        // Import sekce
        ImportCard(
            importState = importState,
            onImport = {
                importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            },
            onConfirmImport = { replaceExisting ->
                viewModel.importFromAnalyzedFile(replaceExisting)
            },
            onResetImport = { viewModel.resetImportState() }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Icons.Default.ImportExport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Záloha a obnovení vzorů",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = "Exportujte své naučené zvukové vzory pro zálohu nebo sdílení. " +
                      "Importujte vzory z jiného zařízení nebo ze zálohy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ExportCard(
    availablePatternsCount: Int,
    exportState: ExportImportViewModel.ExportState,
    onExport: () -> Unit,
    onResetExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Export vzorů",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "K dispozici: $availablePatternsCount vzorů",
                style = MaterialTheme.typography.bodyMedium,
                color = if (availablePatternsCount > 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            when (exportState) {
                ExportImportViewModel.ExportState.Idle -> {
                    Button(
                        onClick = onExport,
                        enabled = availablePatternsCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORTOVAT VZORY")
                    }

                    if (availablePatternsCount == 0) {
                        Text(
                            text = "Nejdřív nahrajte a uložte některé zvukové vzory",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ExportImportViewModel.ExportState.InProgress -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Probíhá export...")
                    }
                }

                is ExportImportViewModel.ExportState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Export úspěšný!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                "Soubor byl uložen jako: ${exportState.fileName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onResetExport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("EXPORTOVAT ZNOVU")
                    }
                }

                is ExportImportViewModel.ExportState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Chyba při exportu",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                exportState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onResetExport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ZKUSIT ZNOVU")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportCard(
    importState: ExportImportViewModel.ImportState,
    onImport: () -> Unit,
    onConfirmImport: (Boolean) -> Unit,
    onResetImport: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
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
                    Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Import vzorů",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            when (importState) {
                ExportImportViewModel.ImportState.Idle -> {
                    Button(
                        onClick = onImport,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VYBRAT EXPORT SOUBOR")
                    }
                }

                ExportImportViewModel.ImportState.AnalyzingFile -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Analyzuji soubor...")
                    }
                }

                is ExportImportViewModel.ImportState.FileAnalyzed -> {
                    ImportPreviewCard(
                        exportInfo = importState.exportInfo,
                        duplicates = importState.duplicates,
                        onConfirmImport = onConfirmImport,
                        onCancel = onResetImport
                    )
                }

                ExportImportViewModel.ImportState.InProgress -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Probíhá import...")
                    }
                }

                is ExportImportViewModel.ImportState.Success -> {
                    ImportResultCard(
                        result = importState.result,
                        onReset = onResetImport
                    )
                }

                is ExportImportViewModel.ImportState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Chyba při importu",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                importState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onResetImport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ZKUSIT ZNOVU")
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun ImportPreviewCard(
    exportInfo: com.example.acusen.storage.PatternExportImportService.ExportData,
    duplicates: List<String>,
    onConfirmImport: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val exportDate = dateFormat.format(Date(exportInfo.exportedAt))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Náhled importu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vzory v exportu:")
                Text(
                    "${exportInfo.totalPatterns}",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Export vytvořen:")
                Text(exportDate, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Zařízení:")
                Text(exportInfo.metadata.deviceModel, fontWeight = FontWeight.Medium)
            }

            if (duplicates.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "⚠️ Duplikátní vzory (${duplicates.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                duplicates.take(3).forEach { duplicate ->
                    Text(
                        "• $duplicate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (duplicates.size > 3) {
                    Text(
                        "... a ${duplicates.size - 3} dalších",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ZRUŠIT")
                }

                Button(
                    onClick = { onConfirmImport(false) },
                    modifier = Modifier.weight(1f),
                    enabled = duplicates.isEmpty()
                ) {
                    Text("IMPORTOVAT")
                }

                if (duplicates.isNotEmpty()) {
                    Button(
                        onClick = { onConfirmImport(true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("PŘEPSAT", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportResultCard(
    result: com.example.acusen.storage.PatternExportImportService.ImportResult,
    onReset: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Import dokončen!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Importováno vzorů:")
                Text(
                    "${result.importedCount}",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (result.skippedCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Přeskočeno (duplikáty):")
                    Text("${result.skippedCount}", fontWeight = FontWeight.Medium)
                }
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("IMPORTOVAT DALŠÍ")
            }
        }
    }
}

