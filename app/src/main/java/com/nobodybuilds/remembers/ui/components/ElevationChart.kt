package com.nobodybuilds.remembers.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.nobodybuilds.remembers.ui.formatDistance
import com.nobodybuilds.remembers.ui.formatMetres
import com.nobodybuilds.remembers.ui.theme.AppTheme
import com.nobodybuilds.remembers.ui.theme.HardHat
import com.nobodybuilds.remembers.ui.theme.NightShift
import kotlin.math.max

/**
 * Height against distance along the trail. [marker] (metres along) shows where you are.
 */
@Composable
fun ElevationChart(
    distances: DoubleArray,
    altitudes: List<Double?>,
    modifier: Modifier = Modifier,
    marker: Double? = null,
) {
    val samples = distances.indices.mapNotNull { i -> altitudes[i]?.let { distances[i] to it } }
    if (samples.size < 2) return
    val colors = MaterialTheme.colorScheme
    val status = AppTheme.status
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val low = samples.minOf { it.second }
    val high = samples.maxOf { it.second }
    val total = max(samples.last().first, 1.0)

    Canvas(modifier.semantics { contentDescription = "Elevation from ${formatMetres(low)} to ${formatMetres(high)}" }) {
        val left = 44.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val span = max(high - low, 20.0)
        val base = low - (span - (high - low)) / 2
        fun x(d: Double) = (left + (d / total) * (size.width - left)).toFloat()
        fun y(a: Double) = (bottom - (a - base) / span * (bottom - top)).toFloat()

        drawLine(status.line, Offset(left, top), Offset(size.width, top), 1f)
        drawLine(status.line, Offset(left, bottom), Offset(size.width, bottom), 1f)
        val line = Path()
        samples.forEachIndexed { i, (d, a) -> if (i == 0) line.moveTo(x(d), y(a)) else line.lineTo(x(d), y(a)) }
        val area = Path().apply {
            addPath(line)
            lineTo(x(samples.last().first), bottom)
            lineTo(x(samples.first().first), bottom)
            close()
        }
        drawPath(area, colors.primaryContainer)
        drawPath(line, colors.primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawText(measurer.measure(formatMetres(high), style), topLeft = Offset(0f, top - 6.dp.toPx()))
        drawText(measurer.measure(formatMetres(low), style), topLeft = Offset(0f, bottom - 10.dp.toPx()))
        drawText(measurer.measure("0", style), topLeft = Offset(left, bottom + 4.dp.toPx()))
        val end = measurer.measure(formatDistance(total), style)
        drawText(end, topLeft = Offset(size.width - end.size.width, bottom + 4.dp.toPx()))

        marker?.let { m ->
            val d = m.coerceIn(0.0, total)
            val next = samples.indexOfFirst { it.first >= d }.coerceAtLeast(1)
            val (d0, a0) = samples[next - 1]
            val (d1, a1) = samples[next]
            val a = if (d1 == d0) a1 else a0 + (a1 - a0) * (d - d0) / (d1 - d0)
            drawLine(NightShift.copy(alpha = 0.35f), Offset(x(d), top), Offset(x(d), bottom), 1.5.dp.toPx())
            drawCircle(NightShift, 7.dp.toPx(), Offset(x(d), y(a)))
            drawCircle(HardHat, 5.dp.toPx(), Offset(x(d), y(a)))
        }
    }
}
