package com.nobodybuilds.remembers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nobodybuilds.remembers.data.Trail
import com.nobodybuilds.remembers.ui.formatDate
import com.nobodybuilds.remembers.ui.formatDistance
import com.nobodybuilds.remembers.ui.formatDuration
import com.nobodybuilds.remembers.ui.formatMetres
import com.nobodybuilds.remembers.ui.theme.AppTheme

fun Trail.summary(): String = buildList {
    add(formatDistance(distance))
    if (ascent >= 1) add("↑ ${formatMetres(ascent)}")
    endedAt?.let { add(formatDuration(it - startedAt)) } ?: add("open")
}.joinToString(" · ")

/** One trail in a segmented list: rounded ends on the first and last, small gaps between. */
@Composable
fun TrailRow(trail: Trail, index: Int, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.colors(containerColor = AppTheme.status.panel),
        leadingContent = {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        },
        overlineContent = { Text(formatDate(trail.startedAt)) },
        supportingContent = { Text(trail.summary()) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
    ) { Text(trail.name) }
}
