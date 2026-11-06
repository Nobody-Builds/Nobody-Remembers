package com.nobodybuilds.remembers.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FlutterDash
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nobodybuilds.remembers.R
import com.nobodybuilds.remembers.data.MarkType
import com.nobodybuilds.remembers.tracking.Fix
import com.nobodybuilds.remembers.ui.theme.AppTheme

val MarkType.icon: ImageVector
    get() = when (this) {
        MarkType.BIRD -> Icons.Rounded.FlutterDash
        MarkType.JUNCTION -> Icons.AutoMirrored.Rounded.CallSplit
        MarkType.WATER -> Icons.Rounded.WaterDrop
        MarkType.VIEW -> Icons.Rounded.Landscape
        MarkType.HAZARD -> Icons.Rounded.Warning
        MarkType.NOTE -> Icons.Rounded.EditNote
    }

enum class Tone { Success, Warn, Info, Brand }

/** A word on a soft fill, with an optional icon. Never colour alone. */
@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null, loading: Boolean = false) {
    val s = AppTheme.status
    val (fill, ink) = when (tone) {
        Tone.Success -> s.successSoft to s.successInk
        Tone.Warn -> s.warnSoft to s.warnInk
        Tone.Info -> s.infoSoft to s.infoInk
        Tone.Brand -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(color = fill, contentColor = ink, shape = CircleShape, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                loading -> LoadingIndicator(Modifier.size(18.dp), color = ink)
                icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GpsPill(fix: Fix?, modifier: Modifier = Modifier) {
    when {
        fix == null -> StatusPill("Finding GPS", Tone.Info, modifier, loading = true)
        fix.accuracy <= 15 -> StatusPill("GPS ±${fix.accuracy.toInt()} m", Tone.Success, modifier)
        else -> StatusPill("Weak GPS ±${fix.accuracy.toInt()} m", Tone.Warn, modifier)
    }
}

@Composable
fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(color = AppTheme.status.panel, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** A card-like panel in the kit's style: white (or raised navy) with a 16 dp radius and no shadow. */
@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = AppTheme.status.panel, content: @Composable () -> Unit) {
    Surface(color = color, shape = MaterialTheme.shapes.medium, modifier = modifier, content = content)
}

/** The sleeping builder, for screens with nothing on them yet. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, builderHeight: Dp = 140.dp) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(R.drawable.builder),
            contentDescription = "The builder, asleep on a half-built wall",
            modifier = Modifier.padding(builderHeight / 4).heightIn(max = builderHeight),
        )
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Shown while the tracking service spins up. Gives up and calls [onGiveUp] if it never does. */
@Composable
fun StartingUp(onGiveUp: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(6_000)
        onGiveUp()
    }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(top = 160.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator(Modifier.size(64.dp))
    }
}
