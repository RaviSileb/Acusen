package com.example.acusen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import com.example.acusen.ui.screens.AlertScreen
import com.example.acusen.ui.theme.AcusenTheme

/**
 * Activity pro zobrazení červeného alertu při detekci alarmu
 */
class AlertActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_PATTERN_NAME = "pattern_name"
        private const val EXTRA_CONFIDENCE = "confidence"
        private const val EXTRA_TIMESTAMP = "timestamp"

        fun createIntent(
            context: Context,
            patternName: String,
            confidence: Float,
            timestamp: Long = System.currentTimeMillis()
        ): Intent {
            return Intent(context, AlertActivity::class.java).apply {
                putExtra(EXTRA_PATTERN_NAME, patternName)
                putExtra(EXTRA_CONFIDENCE, confidence)
                putExtra(EXTRA_TIMESTAMP, timestamp)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nastavení pro zobrazení přes jiné aplikace
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Moderní způsob handling back pressu
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Zabránit zavření pomocí back buttonu při kritickém alertu
                // Uživatel musí potvrdit alarm tlačítkem
            }
        })

        val patternName = intent.getStringExtra(EXTRA_PATTERN_NAME) ?: "Neznámý alarm"
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        setContent {
            AcusenTheme {
                AlertScreen(
                    patternName = patternName,
                    confidence = confidence,
                    timestamp = timestamp,
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}

