package com.nobodybuilds.remembers.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nobodybuilds.remembers.tracking.Session
import com.nobodybuilds.remembers.tracking.Tracker
import com.nobodybuilds.remembers.ui.screens.GuideScreen
import com.nobodybuilds.remembers.ui.screens.HomeScreen
import com.nobodybuilds.remembers.ui.screens.RecordScreen
import com.nobodybuilds.remembers.ui.screens.TrailDetailScreen
import com.nobodybuilds.remembers.ui.screens.TrailsScreen

private const val MAIN = "main"
private const val RECORD = "record"
private const val GUIDE = "guide"
private const val TRAIL = "trail/{id}"

private fun NavHostController.openTrail(id: Long) = navigate("trail/$id") { launchSingleTop = true }

fun NavHostController.openRecord() = navigate(RECORD) {
    popUpTo(MAIN)
    launchSingleTop = true
}

/** Opens whatever screen the running session belongs to, sitting on top of the main tabs. */
fun NavHostController.openSession() {
    val route = when (Tracker.session.value) {
        is Session.Recording -> RECORD
        is Session.Guiding -> GUIDE
        Session.Idle -> return
    }
    navigate(route) {
        popUpTo(MAIN)
        launchSingleTop = true
    }
}

@Composable
fun AppNav(nav: NavHostController = rememberNavController()) {
    NavHost(nav, startDestination = MAIN) {
        composable(MAIN) {
            MainTabs(
                onOpenSession = { nav.openSession() },
                onOpenRecord = { nav.openRecord() },
                onOpenTrail = { nav.openTrail(it) },
            )
        }
        composable(RECORD) {
            RecordScreen(
                onBack = { nav.popBackStack(MAIN, inclusive = false) },
                onGuide = { nav.navigate(GUIDE) { popUpTo(MAIN) } },
                onFinished = { id -> nav.navigate("trail/$id") { popUpTo(MAIN) } },
            )
        }
        composable(GUIDE) {
            GuideScreen(
                onBack = { nav.popBackStack(MAIN, inclusive = false) },
                onDone = { id -> nav.navigate("trail/$id") { popUpTo(MAIN) } },
            )
        }
        composable(TRAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            TrailDetailScreen(
                trailId = entry.arguments!!.getLong("id"),
                onBack = { nav.popBackStack() },
                onGuide = { nav.navigate(GUIDE) { popUpTo(MAIN) } },
            )
        }
    }
}

@Composable
private fun MainTabs(onOpenSession: () -> Unit, onOpenRecord: () -> Unit, onOpenTrail: (Long) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                ShortNavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Rounded.Hiking, contentDescription = null) },
                    label = { Text("Hike") },
                )
                ShortNavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Rounded.Route, contentDescription = null) },
                    label = { Text("Trails") },
                )
            }
        },
    ) { padding ->
        AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { current ->
            when (current) {
                0 -> HomeScreen(padding, onOpenSession, onOpenRecord, onOpenTrail, onSeeAll = { tab = 1 })
                else -> TrailsScreen(padding, onOpenTrail)
            }
        }
    }
}
