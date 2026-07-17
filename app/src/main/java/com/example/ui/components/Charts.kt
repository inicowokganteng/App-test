package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun SimpleBarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    warningColor: Color = MaterialTheme.colorScheme.error
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = labelColor, fontSize = 12.sp)

    Canvas(modifier = modifier.height(150.dp).padding(vertical = 16.dp)) {
        if (data.isEmpty()) return@Canvas

        val maxAmount = max(data.maxOf { it.second }, 1.0)
        
        // Calculate average for non-zero data to determine threshold
        val nonZeroData = data.filter { it.second > 0 }
        val average = if (nonZeroData.isNotEmpty()) nonZeroData.map { it.second }.average() else 0.0
        val threshold = average * 1.25 // 25% above average is considered "wasteful"
        
        val barWidth = size.width / (data.size * 2f)
        val spacing = barWidth

        data.forEachIndexed { index, pair ->
            val (label, amount) = pair
            val barHeight = (amount / maxAmount) * (size.height - 40f) // leave space for text
            val xOffset = index * (barWidth + spacing) + spacing / 2

            val colorToUse = if (amount > threshold && amount > 0) warningColor else barColor

            // Draw Bar
            drawRoundRect(
                color = colorToUse,
                topLeft = Offset(xOffset, size.height - barHeight.toFloat() - 30f),
                size = Size(barWidth, barHeight.toFloat()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Draw Label
            val textLayoutResult = textMeasurer.measure(label, textStyle)
            val textWidth = textLayoutResult.size.width
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = textStyle,
                topLeft = Offset(xOffset + (barWidth - textWidth) / 2, size.height - 20f)
            )
        }
    }
}
