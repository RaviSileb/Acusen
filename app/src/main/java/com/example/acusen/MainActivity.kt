package com.example.acusen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.acusen.ui.screens.MainScreen
import com.example.acusen.ui.theme.AcusenTheme
import com.example.acusen.viewmodel.MonitoringViewModel

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Callback po udělení/odmítnutí oprávnění
        updatePermissionStatus(permissions)
    }

    private var monitoringViewModel: MonitoringViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Zkontroluje a požádá o oprávnění při startu
        checkAndRequestPermissions()

        setContent {
            AcusenTheme {
                val viewModel: MonitoringViewModel = viewModel()
                monitoringViewModel = viewModel

                // Aktualizuje oprávnění ve ViewModel
                LaunchedEffect(Unit) {
                    updatePermissionStatus()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            updatePermissionStatus()
        }
    }

    private fun updatePermissionStatus(grantedPermissions: Map<String, Boolean>? = null) {
        val permissionStatus = if (grantedPermissions != null) {
            grantedPermissions
        } else {
            requiredPermissions.associateWith { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        monitoringViewModel?.updatePermissions(permissionStatus)
    }
}