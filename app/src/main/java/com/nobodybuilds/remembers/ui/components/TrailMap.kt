package com.nobodybuilds.remembers.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.nobodybuilds.remembers.data.Mark
import com.nobodybuilds.remembers.data.MarkType
import com.nobodybuilds.remembers.geo.LocalProjection
import com.nobodybuilds.remembers.geo.RoutePoint
import com.nobodybuilds.remembers.tracking.Fix
import com.nobodybuilds.remembers.ui.formatDistance
import com.nobodybuilds.remembers.ui.theme.AppTheme
import com.nobodybuilds.remembers.ui.theme.HardHat
import com.nobodybuilds.remembers.ui.theme.NightShift
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The trail drawn to scale, north up, with no map tiles: everything here works with no signal.
 * Pinch to zoom, drag to pan, tap the button to fit again.
 *
 * @param walkedUpTo points before this index are drawn as already walked.
 * @param followMetres when set, frames this many metres around [fix] instead of the whole trail.
 */
@Composable
fun TrailMap(
    points: List<RoutePoint>,
    modifier: Modifier = Modifier,
    marks: List<Mark> = emptyList(),
    fix: Fix? = null,
    heading: Double? = null,
    walkedUpTo: Int? = null,
    showEnd: Boolean = false,
    followMetres: Double? = null,
) {
    val colors = MaterialTheme.colorScheme
    val status = AppTheme.status
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    Box(modifier.semantics { contentDescription = "Trail map, north up" }) {
        Canvas(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.5f, 30f)
                        pan += panChange
                    }
                },
        ) {
            drawRect(colors.surfaceContainer)
            val origin = points.firstOrNull()?.let { it.lat to it.lon } ?: fix?.let { it.lat to it.lon } ?: return@Canvas
            val proj = LocalProjection(origin.first, origin.second)

            // Frame: the whole trail, or a box around you.
            val xs = ArrayList<Double>()
            val ys = ArrayList<Double>()
            if (followMetres != null && fix != null) {
                val fx = proj.x(fix.lon)
                val fy = proj.y(fix.lat)
                xs += listOf(fx - followMetres, fx + followMetres)
                ys += listOf(fy - followMetres, fy + followMetres)
            } else {
                points.forEach { xs += proj.x(it.lon); ys += proj.y(it.lat) }
                fix?.let { xs += proj.x(it.lon); ys += proj.y(it.lat) }
            }
            val minSpan = 80.0
            val cx = (xs.min() + xs.max()) / 2
            val cy = (ys.min() + ys.max()) / 2
            val spanX = max(xs.max() - xs.min(), minSpan)
            val spanY = max(ys.max() - ys.min(), minSpan)
            val pad = 28.dp.toPx()
            val baseScale = min((size.width - 2 * pad) / spanX, (size.height - 2 * pad) / spanY)
            val scale = baseScale * zoom
            fun toScreen(lat: Double, lon: Double) = Offset(
                (size.width / 2 + (proj.x(lon) - cx) * scale).toFloat() + pan.x,
                (size.height / 2 - (proj.y(lat) - cy) * scale).toFloat() + pan.y,
            )

            val gridStep = niceStep(size.width / scale / 4)
            drawGrid(gridStep, scale, cx, cy, pan, status.line.copy(alpha = 0.6f))

            if (points.size >= 2) {
                val split = (walkedUpTo ?: 0).coerceIn(0, points.size - 1)
                val walked = Path()
                val ahead = Path()
                points.forEachIndexed { i, p ->
                    val o = toScreen(p.lat, p.lon)
                    if (i <= split) { if (i == 0) walked.moveTo(o.x, o.y) else walked.lineTo(o.x, o.y) }
                    if (i >= split) { if (i == split) ahead.moveTo(o.x, o.y) else ahead.lineTo(o.x, o.y) }
                }
                val outline = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val line = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                if (split > 0) {
                    drawPath(walked, status.panel, style = outline)
                    drawPath(walked, colors.outline, style = line)
                }
                drawPath(ahead, status.panel, style = outline)
                drawPath(ahead, colors.primary, style = line)
            }

            marks.forEach { m ->
                val o = toScreen(m.lat, m.lon)
                drawCircle(NightShift, 7.dp.toPx(), o)
                drawCircle(markColor(m), 5.dp.toPx(), o)
            }
            points.firstOrNull()?.let { start ->
                val o = toScreen(start.lat, start.lon)
                drawCircle(status.panel, 8.dp.toPx(), o)
                drawCircle(if (showEnd) colors.outline else status.successInk, 6.dp.toPx(), o)
            }
            if (showEnd && points.size >= 2) {
                val o = toScreen(points.last().lat, points.last().lon)
                drawCircle(NightShift, 11.dp.toPx(), o)
                drawCircle(HardHat, 9.dp.toPx(), o)
                drawCircle(NightShift, 3.dp.toPx(), o)
            }

            fix?.let { f ->
                val o = toScreen(f.lat, f.lon)
                drawCircle(colors.primary.copy(alpha = 0.14f), (f.accuracy * scale).toFloat().coerceAtLeast(12.dp.toPx()), o)
                heading?.let { h -> drawHeadingCone(o, h, colors.primary) }
                drawCircle(status.panel, 9.dp.toPx(), o)
                drawCircle(colors.primary, 6.5.dp.toPx(), o)
            }

            drawNorth(measurer, labelStyle, colors.onSurfaceVariant)
            drawScaleBar(measurer, labelStyle, gridStep, scale, colors.onSurfaceVariant)
        }
        if (zoom != 1f || pan != Offset.Zero) {
            FilledTonalIconButton(
                onClick = { zoom = 1f; pan = Offset.Zero },
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            ) { Icon(Icons.Rounded.CenterFocusStrong, contentDescription = "Fit the trail") }
        }
    }
}

private fun markColor(mark: Mark): Color = when (mark.type) {
    MarkType.WATER -> Color(0xFF7FA8E8)
    MarkType.HAZARD -> Color(0xFFEA724A)
    MarkType.BIRD -> Color(0xFFA6D98F)
    else -> HardHat
}

/** 1, 2 or 5 × a power of ten, near [raw] metres. */
internal fun niceStep(raw: Double): Double {
    if (raw <= 0) return 10.0
    val p = 10.0.pow(kotlin.math.floor(log10(raw)))
    val f = raw / p
    return p * when {
        f < 1.5 -> 1.0
        f < 3.5 -> 2.0
        f < 7.5 -> 5.0
        else -> 10.0
    }
}

private fun DrawScope.drawGrid(step: Double, scale: Double, cx: Double, cy: Double, pan: Offset, color: Color) {
    val px = (step * scale).toFloat()
    if (px < 12f) return
    val offX = ((size.width / 2 - (cx % step) * scale).toFloat() + pan.x).mod(px)
    val offY = ((size.height / 2 + (cy % step) * scale).toFloat() + pan.y).mod(px)
    var x = offX
    while (x < size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f); x += px }
    var y = offY
    while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 1f); y += px }
}

private fun DrawScope.drawHeadingCone(center: Offset, heading: Double, color: Color) {
    val r = 34.dp.toPx()
    val spread = 28.0
    val path = Path().apply {
        moveTo(center.x, center.y)
        for (a in listOf(-spread, spread)) {
            val rad = Math.toRadians(heading + a - 90)
            lineTo(center.x + (r * cos(rad)).toFloat(), center.y + (r * sin(rad)).toFloat())
        }
        close()
    }
    drawPath(path, color.copy(alpha = 0.28f))
}

private fun DrawScope.drawNorth(measurer: TextMeasurer, style: TextStyle, color: Color) {
    val c = Offset(size.width - 22.dp.toPx(), 26.dp.toPx())
    val h = 9.dp.toPx()
    drawPath(Path().apply {
        moveTo(c.x, c.y - h); lineTo(c.x + h * 0.6f, c.y + h * 0.6f); lineTo(c.x, c.y + h * 0.2f)
        lineTo(c.x - h * 0.6f, c.y + h * 0.6f); close()
    }, color)
    val label = measurer.measure("N", style)
    drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y + h))
}

private fun DrawScope.drawScaleBar(measurer: TextMeasurer, style: TextStyle, step: Double, scale: Double, color: Color) {
    val len = (step * scale).toFloat()
    if (len !in 8f..size.width * 0.6f) return
    val y = size.height - 14.dp.toPx()
    val x = 14.dp.toPx()
    drawLine(color, Offset(x, y), Offset(x + len, y), 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, Offset(x, y - 4.dp.toPx()), Offset(x, y), 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, Offset(x + len, y - 4.dp.toPx()), Offset(x + len, y), 2.dp.toPx(), cap = StrokeCap.Round)
    val label = measurer.measure(if (step < 1000) "${step.toInt()} m" else formatDistance(step), style)
    drawText(label, topLeft = Offset(x, y - 6.dp.toPx() - label.size.height))
}
