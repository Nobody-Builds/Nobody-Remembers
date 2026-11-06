package com.nobodybuilds.remembers.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nobodybuilds.remembers.data.Gpx
import com.nobodybuilds.remembers.data.label
import com.nobodybuilds.remembers.geo.Route
import com.nobodybuilds.remembers.geo.RoutePoint
import com.nobodybuilds.remembers.trailDao
import com.nobodybuilds.remembers.tracking.Session
import com.nobodybuilds.remembers.tracking.Tracker
import com.nobodybuilds.remembers.tracking.TrackingService
import com.nobodybuilds.remembers.ui.components.ElevationChart
import com.nobodybuilds.remembers.ui.components.Panel
import com.nobodybuilds.remembers.ui.components.StatTile
import com.nobodybuilds.remembers.ui.components.TrailMap
import com.nobodybuilds.remembers.ui.components.icon
import com.nobodybuilds.remembers.ui.components.rememberLocationAccess
import com.nobodybuilds.remembers.ui.formatDate
import com.nobodybuilds.remembers.ui.formatDistance
import com.nobodybuilds.remembers.ui.formatDuration
import com.nobodybuilds.remembers.ui.formatMetres
import com.nobodybuilds.remembers.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TrailDetailScreen(trailId: Long, onBack: () -> Unit, onGuide: () -> Unit) {
    val context = LocalContext.current
    val dao = context.trailDao
    val scope = rememberCoroutineScope()
    val trail by remember(trailId) { dao.observeTrail(trailId) }.collectAsStateWithLifecycle(null)
    val points by remember(trailId) { dao.observePoints(trailId) }.collectAsStateWithLifecycle(emptyList())
    val marks by remember(trailId) { dao.observeMarks(trailId) }.collectAsStateWithLifecycle(emptyList())
    val session by Tracker.session.collectAsStateWithLifecycle()
    val access = rememberLocationAccess()
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var menu by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var pendingGuide by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val route = remember(points) { points.takeIf { it.size >= 2 }?.let { list -> Route(list.map { RoutePoint(it.lat, it.lon, it.alt) }) } }
    val inUse = when (val s = session) {
        is Session.Recording -> s.trailId == trailId
        is Session.Guiding -> s.trailId == trailId
        Session.Idle -> false
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri ->
        val t = trail ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val gpx = Gpx.write(t, dao.points(t.id), dao.marks(t.id))
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(gpx.toByteArray()) }
                }.isSuccess
            }
            snackbar.showSnackbar(if (ok) "GPX saved" else "Couldn't save the GPX file")
        }
    }

    fun startGuide(toStart: Boolean) = access.request {
        TrackingService.guide(context, trailId, toStart)
        onGuide()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(trail?.name ?: "") },
                subtitle = { trail?.let { Text(formatDate(it.startedAt)) } },
                scrollBehavior = scroll,
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { trail?.let { export.launch(fileName(it.name, it.startedAt)) } }) {
                        Icon(Icons.Rounded.IosShare, contentDescription = "Export GPX")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = { menu = false; renaming = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                enabled = !inUse,
                                onClick = { menu = false; deleting = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val canGuide = route != null && !inUse
                Button(
                    onClick = { if (session == Session.Idle) startGuide(true) else pendingGuide = true },
                    enabled = canGuide,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
                    modifier = Modifier.weight(1f).heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.Rounded.UTurnLeft, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Back to start")
                }
                OutlinedButton(
                    onClick = { if (session == Session.Idle) startGuide(false) else pendingGuide = false },
                    enabled = canGuide,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.Rounded.NearMe, contentDescription = null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Follow it again")
                }
            }
        },
    ) { padding ->
        val t = trail ?: return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Panel(Modifier.fillMaxWidth().height(300.dp)) {
                    TrailMap(points = route?.points.orEmpty(), marks = marks, showEnd = true, modifier = Modifier.fillMaxSize())
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Distance", formatDistance(t.distance), Icons.Rounded.Straighten, Modifier.weight(1f))
                        StatTile("Time", t.endedAt?.let { formatDuration(it - t.startedAt) } ?: "Open", Icons.Rounded.Schedule, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Climbed", formatMetres(t.ascent), Icons.AutoMirrored.Rounded.TrendingUp, Modifier.weight(1f))
                        StatTile("Descended", formatMetres(t.descent), Icons.AutoMirrored.Rounded.TrendingDown, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Highest", t.maxAlt?.let(::formatMetres) ?: "–", Icons.Rounded.Terrain, Modifier.weight(1f))
                        StatTile("Lowest", t.minAlt?.let(::formatMetres) ?: "–", Icons.Rounded.Height, Modifier.weight(1f))
                    }
                }
            }
            if (route?.hasAltitude == true) {
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Elevation", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (t.barometric) "From the barometer, anchored to GPS" else "From GPS, so expect a few metres of wobble",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            ElevationChart(route.cumulative, route.points.map { it.alt }, Modifier.fillMaxWidth().height(150.dp))
                        }
                    }
                }
            }
            if (marks.isNotEmpty()) {
                item { Text("Marks", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        marks.forEachIndexed { i, m ->
                            ListItem(
                                onClick = {},
                                shapes = ListItemDefaults.segmentedShapes(i, marks.size),
                                colors = ListItemDefaults.colors(containerColor = AppTheme.status.panel),
                                leadingContent = {
                                    Box(
                                        Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(m.type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                                },
                                supportingContent = {
                                    Text(listOfNotNull(clock(m.time), m.alt?.let(::formatMetres)).joinToString(" · "))
                                },
                            ) { Text(m.note.ifBlank { m.type.label }) }
                        }
                    }
                }
            }
            if (route == null) {
                item {
                    Text(
                        "This trail is too short to follow back.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (renaming) {
        var name by rememberSaveable { mutableStateOf(trail?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename trail") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, shape = MaterialTheme.shapes.small)
            },
            confirmButton = {
                Button(
                    onClick = { scope.launch { dao.rename(trailId, name.trim()) }; renaming = false },
                    enabled = name.isNotBlank(),
                    shapes = ButtonDefaults.shapes(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Delete this trail?") },
            text = { Text("Its path and marks go too. This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = { deleting = false; scope.launch { dao.deleteTrail(trailId); onBack() } },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Keep it") } },
        )
    }

    pendingGuide?.let { toStart ->
        AlertDialog(
            onDismissRequest = { pendingGuide = null },
            title = { Text("Stop the current trail?") },
            text = { Text("You're already recording or following a trail. Starting this one stops that.") },
            confirmButton = {
                Button(onClick = { pendingGuide = null; startGuide(toStart) }, shapes = ButtonDefaults.shapes()) { Text("Switch") }
            },
            dismissButton = { TextButton(onClick = { pendingGuide = null }) { Text("Cancel") } },
        )
    }
}

private fun clock(ms: Long) = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

private fun fileName(name: String, startedAt: Long): String {
    val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(Regex("\\s+"), "-")
    return (safe.ifBlank { "trail-$startedAt" }) + ".gpx"
}
