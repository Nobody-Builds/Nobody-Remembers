package com.nobodybuilds.remembers.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nobodybuilds.remembers.data.Mark
import com.nobodybuilds.remembers.data.MarkType
import com.nobodybuilds.remembers.data.label
import com.nobodybuilds.remembers.geo.RoutePoint
import com.nobodybuilds.remembers.trailDao
import com.nobodybuilds.remembers.tracking.Fix
import com.nobodybuilds.remembers.tracking.Session
import com.nobodybuilds.remembers.tracking.Tracker
import com.nobodybuilds.remembers.tracking.TrackingService
import com.nobodybuilds.remembers.ui.components.GpsPill
import com.nobodybuilds.remembers.ui.components.Panel
import com.nobodybuilds.remembers.ui.components.StartingUp
import com.nobodybuilds.remembers.ui.components.StatTile
import com.nobodybuilds.remembers.ui.components.TrailMap
import com.nobodybuilds.remembers.ui.components.icon
import com.nobodybuilds.remembers.ui.components.rememberHeading
import com.nobodybuilds.remembers.ui.formatClock
import com.nobodybuilds.remembers.ui.formatDistance
import com.nobodybuilds.remembers.ui.formatMetres
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RecordScreen(onBack: () -> Unit, onGuide: () -> Unit, onFinished: (Long) -> Unit) {
    val context = LocalContext.current
    val dao = context.trailDao
    val scope = rememberCoroutineScope()
    val session by Tracker.session.collectAsStateWithLifecycle()
    val trailId = (session as? Session.Recording)?.trailId
    // Leave when recording stops from elsewhere (the notification's Stop button).
    var seenRecording by remember { mutableStateOf(false) }
    LaunchedEffect(trailId) {
        if (trailId != null) seenRecording = true
        else if (seenRecording) onBack()
    }
    if (trailId == null) {
        if (!seenRecording) StartingUp(onBack)
        return
    }

    val trail by remember(trailId) { dao.observeTrail(trailId) }.collectAsStateWithLifecycle(null)
    val points by remember(trailId) { dao.observePoints(trailId) }.collectAsStateWithLifecycle(emptyList())
    val marks by remember(trailId) { dao.observeMarks(trailId) }.collectAsStateWithLifecycle(emptyList())
    val fix by Tracker.fix.collectAsStateWithLifecycle()
    val heading by rememberHeading(fix)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }

    var marking by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trail?.name ?: "") },
                subtitle = { Text("Recording · ${formatClock(now - (trail?.startedAt ?: now))}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = { GpsPill(fix, Modifier.padding(end = 12.dp)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Panel(Modifier.weight(1f).fillMaxWidth()) {
                TrailMap(
                    points = points.map { RoutePoint(it.lat, it.lon, it.alt) },
                    marks = marks,
                    fix = fix,
                    heading = heading?.degrees,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            val t = trail
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Distance", formatDistance(t?.distance ?: 0.0), Icons.Rounded.Straighten, Modifier.weight(1f))
                StatTile("Height", fix?.alt?.let(::formatMetres) ?: "–", Icons.Rounded.Height, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Climbed", formatMetres(t?.ascent ?: 0.0), Icons.AutoMirrored.Rounded.TrendingUp, Modifier.weight(1f))
                StatTile("Descended", formatMetres(t?.descent ?: 0.0), Icons.AutoMirrored.Rounded.TrendingDown, Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(toolbarContainerColor = MaterialTheme.colorScheme.secondaryContainer),
                    expandedShadowElevation = 0.dp,
                ) {
                    TextButton(onClick = { marking = true }) {
                        Icon(Icons.Rounded.AddLocationAlt, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Mark")
                    }
                    TextButton(onClick = { confirmFinish = true }) {
                        Icon(Icons.Rounded.Flag, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Finish")
                    }
                }
                Spacer(Modifier.weight(1f))
                MediumExtendedFloatingActionButton(
                    text = { Text("Take me back") },
                    icon = { Icon(Icons.Rounded.UTurnLeft, contentDescription = null) },
                    onClick = {
                        if (points.size < 2) {
                            scope.launch { snackbar.showSnackbar("Walk a few more steps first, so there's a trail to follow back.") }
                        } else {
                            TrackingService.guide(context, trailId, toStart = true)
                            onGuide()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
    }

    if (marking) {
        MarkSheet(
            fix = fix,
            onDismiss = { marking = false },
            onSave = { type, note ->
                val f = fix ?: return@MarkSheet
                scope.launch {
                    dao.insert(Mark(trailId = trailId, time = System.currentTimeMillis(), lat = f.lat, lon = f.lon, alt = f.alt, type = type, note = note.trim()))
                    snackbar.showSnackbar("${type.label} marked")
                }
                marking = false
            },
        )
    }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            icon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
            title = { Text("Finish this trail?") },
            text = { Text("It's saved in Trails, and you can still follow it back from there later.") },
            confirmButton = {
                Button(onClick = { confirmFinish = false; TrackingService.stop(context); onFinished(trailId) }, shapes = ButtonDefaults.shapes()) {
                    Text("Finish")
                }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep recording") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkSheet(fix: Fix?, onDismiss: () -> Unit, onSave: (MarkType, String) -> Unit) {
    var type by rememberSaveable { mutableStateOf(MarkType.BIRD) }
    var note by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Mark this spot", style = MaterialTheme.typography.headlineSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarkType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label) },
                        leadingIcon = { Icon(t.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("What's here? (optional)") },
                placeholder = { Text(if (type == MarkType.BIRD) "Kingfisher by the stream" else "Left fork at the fallen tree") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
            Button(
                onClick = { onSave(type, note) },
                enabled = fix != null,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) { Text(if (fix == null) "Waiting for GPS…" else "Save mark") }
        }
    }
}
