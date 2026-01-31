package com.example.acusen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Graf komponenta pro zobrazení MFCC koeficientů
 */
@Composable
fun MFCCGraph(
    mfccData: FloatArray,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    showLabel: Boolean = true
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showLabel) {
                Text(
                    text = "MFCC Graf zvukové sekvence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (showLabel) 80.dp else 60.dp)
            ) {
                drawMFCCGraph(mfccData, lineColor)
            }
        }
    }
}

private fun DrawScope.drawMFCCGraph(mfccData: FloatArray, lineColor: Color) {
    if (mfccData.isEmpty()) return

    val width = size.width
    val height = size.height
    val padding = 16f
    val graphWidth = width - 2 * padding
    val graphHeight = height - 2 * padding

    // Najít min a max hodnoty pro škálování
    val minValue = mfccData.minOrNull() ?: -2f
    val maxValue = mfccData.maxOrNull() ?: 2f
    val valueRange = maxValue - minValue

    if (valueRange == 0f) return

    // Nakreslit osy
    drawLine(
        color = Color.Gray.copy(alpha = 0.3f),
        start = Offset(padding, height - padding),
        end = Offset(width - padding, height - padding),
        strokeWidth = 1f
    )

    // Nakreslit nulovou linii
    val zeroY = padding + graphHeight * (maxValue / valueRange)
    if (zeroY >= padding && zeroY <= height - padding) {
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(padding, zeroY),
            end = Offset(width - padding, zeroY),
            strokeWidth = 1f
        )
    }

    // Nakreslit MFCC graf
    val path = Path()
    val stepX = graphWidth / (mfccData.size - 1)

    mfccData.forEachIndexed { index, value ->
        val x = padding + index * stepX
        val y = padding + graphHeight * ((maxValue - value) / valueRange)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }

        // Nakreslit body
        drawCircle(
            color = lineColor,
            radius = 2f,
            center = Offset(x, y)
        )
    }

    // Nakreslit spojnice
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 1.5f)
    )
}
