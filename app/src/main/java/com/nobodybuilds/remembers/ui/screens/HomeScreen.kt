package com.nobodybuilds.remembers.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nobodybuilds.remembers.R
import com.nobodybuilds.remembers.data.Trail
import com.nobodybuilds.remembers.trailDao
import com.nobodybuilds.remembers.tracking.Session
import com.nobodybuilds.remembers.tracking.Tracker
import com.nobodybuilds.remembers.tracking.TrackingService
import com.nobodybuilds.remembers.ui.components.Panel
import com.nobodybuilds.remembers.ui.components.StatusPill
import com.nobodybuilds.remembers.ui.components.Tone
import com.nobodybuilds.remembers.ui.components.TrailRow
import com.nobodybuilds.remembers.ui.components.openAppSettings
import com.nobodybuilds.remembers.ui.components.openLocationSettings
import com.nobodybuilds.remembers.ui.components.rememberLocationAccess
import com.nobodybuilds.remembers.ui.components.summary
import com.nobodybuilds.remembers.ui.defaultTrailName
import com.nobodybuilds.remembers.ui.formatDistance
import com.nobodybuilds.remembers.ui.theme.AccentStyle
import com.nobodybuilds.remembers.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenSession: () -> Unit,
    onOpenRecord: () -> Unit,
    onOpenTrail: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    val context = LocalContext.current
    val dao = context.trailDao
    val scope = rememberCoroutineScope()
    val session by Tracker.session.collectAsStateWithLifecycle()
    val guidance by Tracker.guidance.collectAsStateWithLifecycle()
    val trails by remember { dao.observeTrails() }.collectAsStateWithLifecycle(emptyList())
    val unfinished by remember { dao.observeUnfinished() }.collectAsStateWithLifecycle(null)
    val access = rememberLocationAccess()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val activeId = when (val s = session) {
        is Session.Recording -> s.trailId
        is Session.Guiding -> s.trailId
        Session.Idle -> null
    }
    val activeTrail = trails.firstOrNull { it.id == activeId }

    fun startTrail() = access.request {
        scope.launch {
            val now = System.currentTimeMillis()
            unfinished?.let { dao.update(it.copy(endedAt = now)) }
            val id = dao.insert(Trail(name = defaultTrailName(now), startedAt = now))
            TrackingService.record(context, id)
            onOpenRecord()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection).padding(bottom = contentPadding.calculateBottomPadding()),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Nobody Remembers") },
                subtitle = { Text("Your trail does.") },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (session != Session.Idle) {
                item {
                    Panel(color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.padding(16.dp)) {
                            val recording = session is Session.Recording
                            StatusPill(if (recording) "Recording" else "Taking you back", Tone.Brand)
                            Spacer(Modifier.height(10.dp))
                            Text(activeTrail?.name ?: "Your trail", style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (recording) activeTrail?.summary().orEmpty()
                                else guidance?.let { "${formatDistance(it.remaining)} to go" } ?: "Finding where you are",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onOpenSession, shapes = ButtonDefaults.shapes()) {
                                Text("Open")
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            }
                        }
                    }
                }
            }

            val open = unfinished
            if (session == Session.Idle && open != null) {
                item {
                    Panel(color = AppTheme.status.warnSoft) {
                        Column(Modifier.padding(16.dp)) {
                            StatusPill("Left open", Tone.Warn)
                            Spacer(Modifier.height(10.dp))
                            Text(open.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "The app was closed while this was recording (${formatDistance(open.distance)} so far).",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { access.request { TrackingService.record(context, open.id); onOpenRecord() } },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text("Keep recording") }
                                OutlinedButton(
                                    onClick = { scope.launch { dao.update(open.copy(endedAt = System.currentTimeMillis())) } },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text("Finish it") }
                            }
                        }
                    }
                }
            }

            if (session == Session.Idle) {
                item { Hero(onStart = ::startTrail) }
            }

            if (!access.granted && access.asked) {
                item {
                    Problem(
                        title = "Location is off for this app",
                        body = "It needs precise location to record your path and lead you back.",
                        action = "Open settings",
                        onAction = context::openAppSettings,
                    )
                }
            } else if (!access.enabled) {
                item {
                    Problem(
                        title = "Your phone's location is off",
                        body = "Turn it on before you set off. GPS works with no signal at all.",
                        action = "Location settings",
                        onAction = context::openLocationSettings,
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.SatelliteAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Works with no signal. GPS talks to satellites, not cell towers, and nothing here needs the internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (trails.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent trails", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onSeeAll) { Text("All trails") }
                    }
                }
                item {
                    val recent = trails.take(3)
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        recent.forEachIndexed { i, t -> TrailRow(t, i, recent.size, onClick = { onOpenTrail(t.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(onStart: () -> Unit) {
    Panel(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(20.dp)) {
            Image(
                painterResource(R.drawable.ic_logo),
                contentDescription = "The builder at a fork in the trail, scratching his head",
                modifier = Modifier.size(132.dp).align(Alignment.CenterHorizontally),
            )
            Text("Leave a trail.", style = AccentStyle, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(
                "Start before you set off. When it's time to turn round, the app walks you back the way you came: " +
                    "same path, same climbs, every fork you took.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStart,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Rounded.Hiking, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Start a trail", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
        }
    }
}

@Composable
private fun Problem(title: String, body: String, action: String, onAction: () -> Unit) {
    Panel(color = AppTheme.status.warnSoft) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.LocationOff, contentDescription = null, tint = AppTheme.status.warnInk)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onAction, shapes = ButtonDefaults.shapes()) { Text(action) }
            }
        }
    }
}
