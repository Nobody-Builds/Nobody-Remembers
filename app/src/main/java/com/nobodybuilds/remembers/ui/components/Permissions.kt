package com.nobodybuilds.remembers.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

fun Context.hasLocationPermission() =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.locationEnabled() = getSystemService(LocationManager::class.java).isLocationEnabled

fun Context.openAppSettings() =
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

fun Context.openLocationSettings() = startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

class LocationAccess(val granted: Boolean, val enabled: Boolean, val asked: Boolean, val request: (() -> Unit) -> Unit)

/**
 * Location permission (plus notifications, so the trail notification shows). [LocationAccess.request]
 * runs its callback once precise location is granted. Re-checks when you come back from Settings.
 */
@Composable
fun rememberLocationAccess(): LocationAccess {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    var enabled by remember { mutableStateOf(context.locationEnabled()) }
    var asked by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = context.hasLocationPermission()
        enabled = context.locationEnabled()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = context.hasLocationPermission()
        asked = true
        if (granted) pending?.invoke()
        pending = null
    }
    return LocationAccess(granted, enabled, asked) { onGranted ->
        val notificationsOk = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (context.hasLocationPermission() && (notificationsOk || asked)) {
            onGranted()
        } else {
            pending = onGranted
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
            )
        }
    }
}
