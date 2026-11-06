package com.nobodybuilds.remembers.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nobodybuilds.remembers.trailDao
import com.nobodybuilds.remembers.ui.components.EmptyState
import com.nobodybuilds.remembers.ui.components.TrailRow
import com.nobodybuilds.remembers.ui.formatDistance

@Composable
fun TrailsScreen(contentPadding: PaddingValues, onOpenTrail: (Long) -> Unit) {
    val dao = LocalContext.current.trailDao
    val trails by remember { dao.observeTrails() }.collectAsStateWithLifecycle(null)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection).padding(bottom = contentPadding.calculateBottomPadding()),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Trails") },
                subtitle = trails?.takeIf { it.isNotEmpty() }?.let { list ->
                    { Text("${list.size} saved · ${formatDistance(list.sumOf { it.distance })} walked") }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { padding ->
        val list = trails ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                title = "Nothing walked yet",
                body = "The builder's having a nap. Start a trail from Hike and it'll show up here.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
                TrailRow(t, i, list.size, onClick = { onOpenTrail(t.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}
