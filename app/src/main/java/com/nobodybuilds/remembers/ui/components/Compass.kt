package com.nobodybuilds.remembers.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.nobodybuilds.remembers.geo.Geo
import com.nobodybuilds.remembers.ui.theme.AppTheme
import com.nobodybuilds.remembers.ui.theme.NightShift

/**
 * A big arrow pointing where to walk, [degrees] clockwise from the top of the phone.
 * Turns the short way round.
 */
@Composable
fun DirectionArrow(degrees: Double, modifier: Modifier = Modifier, offPath: Boolean = false) {
    val angle = remember { Animatable(degrees.toFloat()) }
    LaunchedEffect(degrees) {
        val delta = Geo.normalize180(degrees - angle.value).toFloat()
        angle.animateTo(angle.value + delta)
    }
    val colors = MaterialTheme.colorScheme
    val status = AppTheme.status
    val fill = if (offPath) status.warnInk else colors.primary
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(status.panel, r)
        drawCircle(status.line, r, style = Stroke(1.5.dp.toPx()))
        for (i in 0 until 12) {
            rotate(i * 30f, c) {
                drawLine(colors.outlineVariant, Offset(c.x, c.y - r + 6.dp.toPx()), Offset(c.x, c.y - r + 14.dp.toPx()), 2.dp.toPx())
            }
        }
        rotate(angle.value, c) {
            val tip = Offset(c.x, c.y - r * 0.78f)
            val arrow = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(c.x + r * 0.42f, c.y + r * 0.42f)
                lineTo(c.x, c.y + r * 0.2f)
                lineTo(c.x - r * 0.42f, c.y + r * 0.42f)
                close()
            }
            drawPath(arrow, fill)
            drawPath(arrow, NightShift, style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}
