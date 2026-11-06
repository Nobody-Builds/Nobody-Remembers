package com.nobodybuilds.remembers.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nobodybuilds.remembers.data.label
import com.nobodybuilds.remembers.geo.Geo
import com.nobodybuilds.remembers.geo.Guidance
import com.nobodybuilds.remembers.trailDao
import com.nobodybuilds.remembers.tracking.Session
import com.nobodybuilds.remembers.tracking.Tracker
import com.nobodybuilds.remembers.tracking.TrackingService
import com.nobodybuilds.remembers.ui.components.DirectionArrow
import com.nobodybuilds.remembers.ui.components.ElevationChart
import com.nobodybuilds.remembers.ui.components.Panel
import com.nobodybuilds.remembers.ui.components.StartingUp
import com.nobodybuilds.remembers.ui.components.StatTile
import com.nobodybuilds.remembers.ui.components.StatusPill
import com.nobodybuilds.remembers.ui.components.Tone
import com.nobodybuilds.remembers.ui.components.TrailMap
import com.nobodybuilds.remembers.ui.components.icon
import com.nobodybuilds.remembers.ui.components.rememberHeading
import com.nobodybuilds.remembers.ui.formatDistance
import com.nobodybuilds.remembers.ui.formatDuration
import com.nobodybuilds.remembers.ui.formatMetres
import com.nobodybuilds.remembers.ui.naismithMillis
import com.nobodybuilds.remembers.ui.theme.AccentStyle
import com.nobodybuilds.remembers.ui.theme.AppTheme
import com.nobodybuilds.remembers.ui.turnHint
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuideScreen(onBack: () -> Unit, onDone: (Long) -> Unit) {
    val context = LocalContext.current
    val dao = context.trailDao
    val session by Tracker.session.collectAsStateWithLifecycle()
    val guiding = session as? Session.Guiding
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(guiding) {
        if (guiding != null) seen = true
        else if (seen) onBack()
    }
    if (guiding == null) {
        if (!seen) StartingUp(onBack)
        return
    }

    val trail by remember(guiding.trailId) { dao.observeTrail(guiding.trailId) }.collectAsStateWithLifecycle(null)
    val marks by remember(guiding.trailId) { dao.observeMarks(guiding.trailId) }.collectAsStateWithLifecycle(emptyList())
    val route by Tracker.route.collectAsStateWithLifecycle()
    val guidance by Tracker.guidance.collectAsStateWithLifecycle()
    val fix by Tracker.fix.collectAsStateWithLifecycle()
    val alerts by Tracker.strayAlerts.collectAsStateWithLifecycle()
    val heading by rememberHeading(fix)
    var nearby by rememberSaveable { mutableStateOf(true) }
    val destination = if (guiding.toStart) "the start" else "the end"

    val markProgress = remember(route, marks) {
        val r = route ?: return@remember emptyList()
        marks.mapNotNull { m -> r.locate(m.lat, m.lon)?.takeIf { it.distance < 40 }?.let { m to it.progress } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (guiding.toStart) "Back to the start" else "On to the end") },
                subtitle = { Text(trail?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToggleButton(
                    checked = alerts,
                    onCheckedChange = { Tracker.strayAlerts.value = it },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    icon = { Icon(Icons.Rounded.Vibration, contentDescription = null) },
                ) { Text(if (alerts) "Buzz if I stray: on" else "Buzz if I stray: off") }
                OutlinedButton(
                    onClick = { TrackingService.stop(context); onBack() },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Stop")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val g = guidance
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    fix == null -> StatusPill("Finding GPS", Tone.Info, loading = true)
                    g == null -> StatusPill("Finding you on the trail", Tone.Info, loading = true)
                    g.arrived -> StatusPill("Arrived", Tone.Success, icon = Icons.Rounded.CheckCircle)
                    g.onPath -> StatusPill("On the path", Tone.Success, icon = Icons.Rounded.CheckCircle)
                    else -> StatusPill("Off the path · ${formatMetres(g.offPath)}", Tone.Warn, icon = Icons.Rounded.Warning)
                }
                fix?.takeIf { it.accuracy > 50 }?.let {
                    StatusPill("Weak GPS ±${it.accuracy.toInt()} m · directions paused", Tone.Warn, icon = Icons.Rounded.Warning)
                }
                if (heading?.needsCalibration == true) StatusPill("Compass: wave phone in a figure 8", Tone.Info, icon = Icons.Rounded.Explore)
            }

            AnimatedVisibility(g?.arrived == true) {
                Panel(color = AppTheme.status.successSoft) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("You made it.", style = AccentStyle, color = AppTheme.status.successInk)
                        Spacer(Modifier.height(6.dp))
                        Text("Same path, every step. Nobody remembered, so the trail did.", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { TrackingService.stop(context); onDone(guiding.trailId) },
                            shapes = ButtonDefaults.shapes(),
                        ) { Text("Done") }
                    }
                }
            }

            if (g != null && !g.arrived) Direction(g, fix?.course?.toDouble()?.takeIf { (fix?.speed ?: 0f) > 0.7f }, heading?.degrees, destination)

            if (g != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Climb left", g.ascentLeft?.let(::formatMetres) ?: "–", Icons.AutoMirrored.Rounded.TrendingUp, Modifier.weight(1f))
                    StatTile("Descent left", g.descentLeft?.let(::formatMetres) ?: "–", Icons.AutoMirrored.Rounded.TrendingDown, Modifier.weight(1f))
                    StatTile("About", formatDuration(naismithMillis(g.remaining, g.ascentLeft ?: 0.0)), Icons.Rounded.Schedule, Modifier.weight(1f))
                }
                markProgress.filter { it.second > g.progress + 5 }.minByOrNull { it.second }?.let { (mark, at) ->
                    Panel(color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(mark.type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Column {
                                Text("${mark.type.label} in ${formatDistance(at - g.progress)}", style = MaterialTheme.typography.titleMedium)
                                if (mark.note.isNotBlank()) Text("“${mark.note}”", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            route?.let { r ->
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(true to "Around me", false to "Whole trail").forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = nearby == value,
                            onClick = { nearby = value },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(label) }
                    }
                }
                val walked = g?.let { gd -> r.cumulative.indexOfFirst { it >= gd.progress }.takeIf { it >= 0 } } ?: 0
                Panel(Modifier.fillMaxWidth().height(300.dp)) {
                    TrailMap(
                        points = r.points,
                        marks = marks,
                        fix = fix,
                        heading = heading?.degrees,
                        walkedUpTo = walked,
                        showEnd = true,
                        followMetres = if (nearby && fix != null) 150.0 else null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (r.hasAltitude) {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Height on the way", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            ElevationChart(r.cumulative, r.points.map { it.alt }, Modifier.fillMaxWidth().height(140.dp), marker = g?.progress)
                        }
                    }
                }
            }
        }
    }
}

/** The arrow and the words: which way, and how far. */
@Composable
private fun Direction(g: Guidance, course: Double?, compass: Double?, destination: String) {
    val facing = compass ?: course
    val relative = facing?.let { Geo.normalize180(g.bearingToTarget - it) }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DirectionArrow(degrees = relative ?: g.bearingToTarget, offPath = !g.onPath, modifier = Modifier.size(200.dp))
            Spacer(Modifier.height(16.dp))
            val instruction = when {
                !g.onPath && relative != null -> "${turnHint(relative)} to rejoin the path"
                !g.onPath -> "Head ${Geo.cardinal(g.bearingToTarget)} to rejoin the path"
                relative != null -> turnHint(relative)
                else -> "Head ${Geo.cardinal(g.bearingToTarget)} (${g.bearingToTarget.roundToInt()}°)"
            }
            Text(
                instruction,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                if (facing == null) "No compass: the arrow points as if the top of the phone faces north." else "${formatDistance(g.remaining)} to $destination",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (facing == null) Text("${formatDistance(g.remaining)} to $destination", style = MaterialTheme.typography.titleMedium)
        }
    }
}
