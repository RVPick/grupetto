package com.spop.poverlay.util

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.spop.poverlay.overlay.OverlaySensorViewModel

/**
 * Line chart of recent sensor values, filling left to right across
 * [OverlaySensorViewModel.GraphMaxDataPoints] slots.
 *
 * Drawn with a plain Canvas: [data] is read only in the draw phase, so a
 * snapshot-state list redraws just the chart when it changes, with no
 * recomposition. (This replaced an embedded chart view that was rebuilt on
 * every update and kept the tablet's CPU busy.)
 *
 * [pauseChart] is kept for callers but no longer needed; redraws are cheap.
 */
@Composable
fun LineChart(
    data: Collection<Number>,
    maxValue: Float,
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") pauseChart: Boolean,
    fillColor: Color = Color.LightGray,
    lineColor: Color = Color.DarkGray,
) {
    Canvas(modifier) {
        if (data.isEmpty() || maxValue <= 0f) return@Canvas
        val strokeWidth = 2.dp.toPx()
        val stepX = size.width / (OverlaySensorViewModel.GraphMaxDataPoints - 1)
        val usableHeight = size.height - strokeWidth
        fun yFor(value: Float) =
            size.height - strokeWidth / 2 - (value.coerceIn(0f, maxValue) / maxValue) * usableHeight

        val line = Path()
        var lastX = 0f
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = yFor(value.toFloat())
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
            lastX = x
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(lastX, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(fillColor, fillColor.copy(alpha = 0f))))
        drawPath(
            line,
            lineColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
