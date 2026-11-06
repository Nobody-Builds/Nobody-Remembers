package com.nobodybuilds.remembers.tracking

import com.nobodybuilds.remembers.geo.Guidance
import com.nobodybuilds.remembers.geo.Route
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface Session {
    data object Idle : Session
    data class Recording(val trailId: Long) : Session
    data class Guiding(val trailId: Long, val toStart: Boolean) : Session
}

data class Fix(
    val lat: Double,
    val lon: Double,
    /** Best altitude we have: barometer anchored to GPS when the phone has one. */
    val alt: Double?,
    val accuracy: Float,
    val speed: Float?,
    val course: Float?,
    val time: Long,
)

/** Live state shared between [TrackingService] and the screens, for this process. */
object Tracker {
    val session = MutableStateFlow<Session>(Session.Idle)
    val fix = MutableStateFlow<Fix?>(null)
    val route = MutableStateFlow<Route?>(null)
    val guidance = MutableStateFlow<Guidance?>(null)
    val strayAlerts = MutableStateFlow(true)
}
