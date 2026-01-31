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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.acusen.alert.AlertManager
import com.example.acusen.viewmodel.AlertViewModel
import kotlinx.coroutines.launch

/**
 * Obrazovka s nastavením aplikace
 */
@Composable
fun SettingsScreen() {
    val alertViewModel: AlertViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val settings by alertViewModel.alertSettings.collectAsState()
    val isTestingEmail by alertViewModel.isTestingEmail.collectAsState()
    val testResult by alertViewModel.testResult.collectAsState()
    val lastAlertInfo by alertViewModel.lastAlertInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Nastavení",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Alert nastavení
        AlertSettingsCard(
            settings = settings,
            onSettingsChange = { newSettings ->
                alertViewModel.updateAlertSettings(newSettings)
            }
        )

        // E-mail konfigurace
        EmailConfigCard(
            settings = settings,
            onSettingsChange = { newSettings ->
                alertViewModel.updateAlertSettings(newSettings)
            },
            onTestEmail = {
                alertViewModel.testEmailAlert()
            },
            isTestingEmail = isTestingEmail,
            testResult = testResult,
            onClearTestResult = {
                alertViewModel.clearTestResult()
            }
        )

        // Pokročilé nastavení
        AdvancedSettingsCard(
            settings = settings,
            onSettingsChange = { newSettings ->
                alertViewModel.updateAlertSettings(newSettings)
            }
        )

        // Poslednn alert info
        LastAlertCard(lastAlertInfo = lastAlertInfo)

        // O aplikaci
        AboutCard()

        // Spacer pro lepší scrollování
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AlertSettingsCard(
    settings: AlertManager.AlertSettings,
    onSettingsChange: (AlertManager.AlertSettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "E-mailové upozornění",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { enabled ->
                        onSettingsChange(settings.copy(isEnabled = enabled))
                    }
                )
            }

            if (settings.isEnabled) {
                Text(
                    text = "Při detekci zvuku bude automaticky odeslán e-mail s audio nahrávkou a GPS lokací.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmailConfigCard(
    settings: AlertManager.AlertSettings,
    onSettingsChange: (AlertManager.AlertSettings) -> Unit,
    onTestEmail: () -> Unit,
    isTestingEmail: Boolean,
    testResult: String?,
    onClearTestResult: () -> Unit
) {
    var showPasswords by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "E-mailová konfigurace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = settings.recipientEmail,
                onValueChange = { email ->
                    onSettingsChange(settings.copy(recipientEmail = email))
                },
                label = { Text("E-mail příjemce") },
                placeholder = { Text("komu@example.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.isEnabled
            )

            OutlinedTextField(
                value = settings.senderEmail,
                onValueChange = { email ->
                    onSettingsChange(settings.copy(senderEmail = email))
                },
                label = { Text("Odesílatelův e-mail") },
                placeholder = { Text("vas@gmail.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.isEnabled
            )

            OutlinedTextField(
                value = settings.senderPassword,
                onValueChange = { password ->
                    onSettingsChange(settings.copy(senderPassword = password))
                },
                label = { Text("Heslo / App Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                enabled = settings.isEnabled,
                trailingIcon = {
                    IconButton(onClick = { showPasswords = !showPasswords }) {
                        Icon(
                            imageVector = if (showPasswords) Icons.Default.Lock else Icons.Default.Build,
                            contentDescription = if (showPasswords) "Skrýt heslo" else "Zobrazit heslo"
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = settings.smtpHost,
                    onValueChange = { host ->
                        onSettingsChange(settings.copy(smtpHost = host))
                    },
                    label = { Text("SMTP Server") },
                    modifier = Modifier.weight(2f),
                    enabled = settings.isEnabled
                )

                OutlinedTextField(
                    value = settings.smtpPort.toString(),
                    onValueChange = { port ->
                        port.toIntOrNull()?.let { portInt ->
                            onSettingsChange(settings.copy(smtpPort = portInt))
                        }
                    },
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    enabled = settings.isEnabled
                )
            }

            // Test button
            if (settings.isEnabled) {
                Button(
                    onClick = onTestEmail,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTestingEmail && settings.recipientEmail.isNotBlank() && settings.senderEmail.isNotBlank()
                ) {
                    if (isTestingEmail) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Odesílám test...")
                    } else {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OTESTOVAT E-MAIL")
                    }
                }
            }

            // Test result
            testResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("✅"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.weight(1f),
                            color = if (result.startsWith("✅"))
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )

                        IconButton(
                            onClick = onClearTestResult,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Zavřít",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsCard(
    settings: AlertManager.AlertSettings,
    onSettingsChange: (AlertManager.AlertSettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pokročilé nastavení",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Minimum confidence
            Column {
                Text(
                    text = "Minimální přesnost detekce: ${(settings.minimumConfidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = settings.minimumConfidence.toFloat(),
                    onValueChange = { confidence ->
                        onSettingsChange(settings.copy(minimumConfidence = confidence.toDouble()))
                    },
                    valueRange = 0.5f..0.95f,
                    enabled = settings.isEnabled
                )
            }

            // Cooldown
            OutlinedTextField(
                value = settings.cooldownMinutes.toString(),
                onValueChange = { cooldown ->
                    cooldown.toIntOrNull()?.let { cooldownInt ->
                        onSettingsChange(settings.copy(cooldownMinutes = cooldownInt))
                    }
                },
                label = { Text("Interval mezi alerty (minuty)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.isEnabled
            )

            // GPS nastavení
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Zahrnout GPS lokaci",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Přiloží GPS souřadnice k alertům a historii",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.includeGPS,
                    onCheckedChange = { includeGPS ->
                        onSettingsChange(settings.copy(includeGPS = includeGPS))
                    },
                    enabled = settings.isEnabled
                )
            }
        }
    }
}

@Composable
private fun LastAlertCard(
    lastAlertInfo: Map<String, Any?>
) {
    val patternName = lastAlertInfo["pattern_name"] as? String
    val confidence = lastAlertInfo["confidence"] as? Float
    val time = lastAlertInfo["time"] as? Long
    val hadLocation = lastAlertInfo["had_location"] as? Boolean ?: false

    if (patternName != null && time != null && time > 0) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Poslední alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vzor:")
                    Text(patternName)
                }

                confidence?.let { conf ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Přesnost:")
                        Text("${(conf * 100).toInt()}%")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Čas:")
                    Text(
                        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(time))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GPS lokace:")
                    Text(if (hadLocation) "✓ Přiložena" else "✗ Nedostupná")
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "O aplikaci",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Acoustic Sentinel v1.0",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "24/7 akustický monitoring s machine learning detekcí zvuků.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Export logs */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export logů")
                }

                OutlinedButton(
                    onClick = { /* TODO: Reset app */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}
