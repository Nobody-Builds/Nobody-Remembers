package com.nobodybuilds.remembers.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import com.nobodybuilds.remembers.MainActivity
import com.nobodybuilds.remembers.R
import com.nobodybuilds.remembers.data.TrackPoint
import com.nobodybuilds.remembers.data.Trail
import com.nobodybuilds.remembers.geo.ClimbCounter
import com.nobodybuilds.remembers.geo.Geo
import com.nobodybuilds.remembers.geo.PathFollower
import com.nobodybuilds.remembers.geo.Route
import com.nobodybuilds.remembers.geo.RoutePoint
import com.nobodybuilds.remembers.trailDao
import com.nobodybuilds.remembers.ui.formatDistance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps GPS running with the screen off. Records a trail, or guides you along one,
 * and buzzes when you stray or arrive.
 */
class TrackingService : Service(), LocationListener, SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private val dao by lazy { trailDao }
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private val altitude = AltitudeFuser()

    private var listening = false
    private var trail: Trail? = null
    private var climb: ClimbCounter? = null
    private var lastPoint: TrackPoint? = null
    private var follower: PathFollower? = null
    private var wasOnPath = true
    private var announcedArrival = false
    private var lastNotified = 0L
    private var lastBuzz = 0L
    private var startId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A newer start replaces the last one; mark the old one done so a restart doesn't replay it.
        if (this.startId != 0) stopSelf(this.startId)
        this.startId = startId
        val trailId = intent?.getLongExtra(EXTRA_TRAIL, -1L) ?: -1L
        when (intent?.action) {
            ACTION_RECORD -> begin(Session.Recording(trailId))
            ACTION_GUIDE -> begin(Session.Guiding(trailId, intent.getBooleanExtra(EXTRA_TO_START, true)))
            else -> { finish(); return START_NOT_STICKY }
        }
        return START_REDELIVER_INTENT
    }

    private fun begin(session: Session) {
        try {
            startForeground(NOTIFICATION_ID, notification(titleFor(session), "Finding GPS…"), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } catch (e: RuntimeException) {
            // The system can refuse a location service restarted from the background. Leave the trail open:
            // the home screen offers to carry on recording it.
            stopSelf()
            return
        }
        if (!startSensors()) return
        scope.launch {
            lock.withLock {
                closeRecording()
                follower = null
                Tracker.route.value = null
                Tracker.guidance.value = null
                when (session) {
                    is Session.Recording -> openRecording(session.trailId) ?: return@withLock stopEverything()
                    is Session.Guiding -> openGuide(session) ?: return@withLock stopEverything()
                    Session.Idle -> Unit
                }
                Tracker.session.value = session
                notifyNow(if (session is Session.Guiding) "Finding you on the trail…" else "Finding GPS…")
            }
        }
    }

    private suspend fun openRecording(trailId: Long): Unit? {
        val t = dao.trail(trailId) ?: return null
        val last = dao.lastPoint(trailId)
        trail = t
        lastPoint = last
        climb = ClimbCounter(climbThreshold(), t.ascent, t.descent, last?.alt)
        return Unit
    }

    private suspend fun openGuide(session: Session.Guiding): Unit? {
        val points = dao.points(session.trailId).map { RoutePoint(it.lat, it.lon, it.alt) }
        if (points.size < 2) return null
        val route = Route(points).let { if (session.toStart) it.reversed() else it }
        follower = PathFollower(route, climbThreshold = climbThreshold())
        wasOnPath = true
        announcedArrival = false
        Tracker.route.value = route
        return Unit
    }

    /** Marks the trail being recorded as finished. */
    private suspend fun closeRecording() {
        val t = trail ?: return
        dao.trail(t.id)?.let { dao.update(it.copy(endedAt = it.endedAt ?: System.currentTimeMillis())) }
        trail = null
        climb = null
        lastPoint = null
    }

    private fun startSensors(): Boolean {
        if (listening) return true
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopEverything()
            return false
        }
        val request = LocationRequest.Builder(1_000L)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(1_000L)
            .build()
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, request, mainExecutor, this)
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        listening = true
        return true
    }

    override fun onLocationChanged(location: Location) {
        val fix = Fix(
            lat = location.latitude,
            lon = location.longitude,
            alt = altitude.altitudeFor(location),
            accuracy = location.accuracy,
            speed = if (location.hasSpeed()) location.speed else null,
            course = if (location.hasBearing()) location.bearing else null,
            time = location.time,
        )
        Tracker.fix.value = fix
        scope.launch { lock.withLock { handle(fix) } }
    }

    private suspend fun handle(fix: Fix) {
        trail?.let { record(it, fix) }
        follower?.let { guide(it, fix) }
    }

    private suspend fun record(current: Trail, fix: Fix) {
        if (fix.accuracy > MAX_RECORD_ACCURACY) return
        val last = lastPoint
        val step = last?.let { Geo.distance(it.lat, it.lon, fix.lat, fix.lon) } ?: 0.0
        if (last != null && step < MIN_STEP_METRES) return

        val point = TrackPoint(trailId = current.id, time = fix.time, lat = fix.lat, lon = fix.lon, alt = fix.alt, accuracy = fix.accuracy)
        dao.insert(point)
        lastPoint = point
        val counter = climb!!
        fix.alt?.let(counter::add)
        val updated = current.copy(
            distance = current.distance + step,
            ascent = counter.ascent,
            descent = counter.descent,
            minAlt = listOfNotNull(current.minAlt, fix.alt).minOrNull(),
            maxAlt = listOfNotNull(current.maxAlt, fix.alt).maxOrNull(),
            barometric = altitude.barometric,
        )
        dao.update(updated)
        trail = updated
        notifyThrottled("Recording · ${formatDistance(updated.distance)}")
    }

    private fun guide(follower: PathFollower, fix: Fix) {
        if (fix.accuracy > MAX_GUIDE_ACCURACY) return
        val g = follower.update(fix.lat, fix.lon)
        Tracker.guidance.value = g
        val alerts = Tracker.strayAlerts.value
        if (g.arrived && !announcedArrival) {
            announcedArrival = true
            buzz(longArrayOf(0, 120, 120, 120, 120, 400))
            notifyNow("You're there.")
            return
        }
        val now = System.currentTimeMillis()
        if (wasOnPath && !g.onPath && alerts && now - lastBuzz > MIN_BUZZ_GAP_MS) {
            lastBuzz = now
            buzz(longArrayOf(0, 500, 200, 500))
        }
        if (!wasOnPath && g.onPath && alerts) buzz(longArrayOf(0, 80))
        wasOnPath = g.onPath
        val text = if (g.onPath) "${formatDistance(g.remaining)} to go" else "Off the path by ${formatDistance(g.offPath)}"
        notifyThrottled(text)
    }

    private fun buzz(pattern: LongArray) {
        getSystemService(VibratorManager::class.java).defaultVibrator
            .vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PRESSURE) altitude.onPressure(event.values[0])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun climbThreshold() = if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) 2.0 else 5.0

    private fun finish() {
        scope.launch {
            lock.withLock {
                closeRecording()
                stopEverything()
            }
        }
    }

    private fun stopEverything() {
        follower = null
        Tracker.session.value = Session.Idle
        Tracker.route.value = null
        Tracker.guidance.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (listening) {
            locationManager.removeUpdates(this)
            sensorManager.unregisterListener(this)
        }
        Tracker.fix.value = null
        scope.cancel()
        super.onDestroy()
    }

    private fun notifyThrottled(text: String) {
        val now = System.currentTimeMillis()
        if (now - lastNotified < 5_000) return
        notifyNow(text)
    }

    private fun notifyNow(text: String) {
        lastNotified = System.currentTimeMillis()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(titleFor(Tracker.session.value), text))
    }

    private fun titleFor(session: Session) = if (session is Session.Guiding) "Taking you back" else "Recording your trail"

    private fun notification(title: String, text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Trail", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, TrackingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_trail)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .build()
    }

    companion object {
        private const val ACTION_RECORD = "record"
        private const val ACTION_GUIDE = "guide"
        private const val ACTION_STOP = "stop"
        private const val EXTRA_TRAIL = "trail"
        private const val EXTRA_TO_START = "toStart"
        private const val CHANNEL = "trail"
        private const val NOTIFICATION_ID = 7
        private const val MAX_RECORD_ACCURACY = 30f
        private const val MAX_GUIDE_ACCURACY = 50f
        private const val MIN_STEP_METRES = 5.0
        private const val MIN_BUZZ_GAP_MS = 20_000L

        fun record(context: Context, trailId: Long) = send(context, ACTION_RECORD, trailId)

        fun guide(context: Context, trailId: Long, toStart: Boolean) =
            send(context, ACTION_GUIDE, trailId) { putExtra(EXTRA_TO_START, toStart) }

        fun stop(context: Context) {
            context.startService(Intent(context, TrackingService::class.java).setAction(ACTION_STOP))
        }

        private fun send(context: Context, action: String, trailId: Long, extra: Intent.() -> Unit = {}) {
            context.startForegroundService(
                Intent(context, TrackingService::class.java).setAction(action).putExtra(EXTRA_TRAIL, trailId).apply(extra),
            )
        }
    }
}
