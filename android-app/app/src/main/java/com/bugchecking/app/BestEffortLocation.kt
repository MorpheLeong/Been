package com.bugchecking.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

internal fun interface LocationRequestHandle {
    fun cancel()
}

internal data class LocationFixQuality(
    val accuracyMeters: Float,
    val ageMillis: Long,
)

internal object LocationFixPolicy {
    const val TARGET_ACCURACY_METERS = 50f
    const val MAX_CACHE_AGE_MILLIS = 120_000L
    private const val IMMEDIATE_CACHE_AGE_MILLIS = 10_000L
    private const val AGE_PENALTY_METERS_PER_SECOND = 2f

    fun score(value: LocationFixQuality): Float =
        value.accuracyMeters.coerceAtLeast(0f) +
            value.ageMillis.coerceAtLeast(0L) / 1_000f * AGE_PENALTY_METERS_PER_SECOND

    fun isImmediate(value: LocationFixQuality): Boolean =
        value.ageMillis <= IMMEDIATE_CACHE_AGE_MILLIS &&
            value.accuracyMeters in 0f..TARGET_ACCURACY_METERS
}

internal object BestEffortLocation {
    private const val DEFAULT_TIMEOUT_MILLIS = 20_000L

    @SuppressLint("MissingPermission")
    fun request(
        context: Context,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        callback: (Location?) -> Unit,
    ): LocationRequestHandle {
        val manager = context.getSystemService(LocationManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            handler.post { callback(null) }
            return LocationRequestHandle {}
        }

        val session = Session(manager, handler, providers, timeoutMillis, callback)
        if (Looper.myLooper() == Looper.getMainLooper()) session.start() else handler.post(session::start)
        return LocationRequestHandle { handler.post(session::cancel) }
    }

    private class Session(
        private val manager: LocationManager,
        private val handler: Handler,
        private val providers: List<String>,
        private val timeoutMillis: Long,
        private val callback: (Location?) -> Unit,
    ) {
        private val cancellationSignals = mutableListOf<CancellationSignal>()
        private val listeners = mutableListOf<LocationListener>()
        private val candidates = mutableListOf<Location>()
        private var remainingProviders = providers.size
        private var completed = false
        private val timeout = Runnable { finish(bestCandidate()) }

        @SuppressLint("MissingPermission")
        fun start() {
            if (completed) return
            val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
            providers.mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }.filter { location -> ageMillis(location, nowElapsedNanos) <= LocationFixPolicy.MAX_CACHE_AGE_MILLIS }
                .forEach(candidates::add)

            bestCandidate()?.takeIf { location ->
                LocationFixPolicy.isImmediate(quality(location, nowElapsedNanos))
            }?.let {
                finish(it)
                return
            }

            handler.postDelayed(timeout, timeoutMillis.coerceAtLeast(1L))
            providers.forEach(::requestProvider)
        }

        fun cancel() {
            if (completed) return
            completed = true
            cleanup()
        }

        @SuppressLint("MissingPermission")
        private fun requestProvider(provider: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cancellationSignals += signal
                try {
                    manager.getCurrentLocation(provider, signal, { task -> handler.post(task) }) { location ->
                        onProviderResult(location)
                    }
                } catch (_: SecurityException) {
                    onProviderResult(null)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        listeners.remove(this)
                        onProviderResult(location)
                    }

                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    @Deprecated("Deprecated by Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                listeners += listener
                try {
                    manager.requestSingleUpdate(provider, listener, handler.looper)
                } catch (_: SecurityException) {
                    listeners.remove(listener)
                    onProviderResult(null)
                }
            }
        }

        private fun onProviderResult(location: Location?) {
            if (completed) return
            remainingProviders--
            if (location != null && isUsable(location)) candidates += location
            if (location != null && LocationFixPolicy.isImmediate(quality(location))) {
                finish(location)
            } else if (remainingProviders <= 0) {
                finish(bestCandidate())
            }
        }

        private fun bestCandidate(): Location? = candidates
            .filter(::isUsable)
            .minByOrNull { LocationFixPolicy.score(quality(it)) }

        private fun finish(location: Location?) {
            if (completed) return
            completed = true
            cleanup()
            callback(location)
        }

        private fun cleanup() {
            handler.removeCallbacks(timeout)
            cancellationSignals.forEach(CancellationSignal::cancel)
            listeners.forEach { listener -> runCatching { manager.removeUpdates(listener) } }
            cancellationSignals.clear()
            listeners.clear()
        }

        private fun isUsable(location: Location): Boolean =
            location.latitude.isFinite() && location.latitude in -90.0..90.0 &&
                location.longitude.isFinite() && location.longitude in -180.0..180.0

        private fun quality(
            location: Location,
            nowElapsedNanos: Long = SystemClock.elapsedRealtimeNanos(),
        ): LocationFixQuality = LocationFixQuality(
            accuracyMeters = if (location.hasAccuracy()) location.accuracy.coerceAtLeast(0f) else 10_000f,
            ageMillis = ageMillis(location, nowElapsedNanos),
        )

        private fun ageMillis(location: Location, nowElapsedNanos: Long): Long = when {
            location.elapsedRealtimeNanos > 0L ->
                ((nowElapsedNanos - location.elapsedRealtimeNanos).coerceAtLeast(0L) / 1_000_000L)
            else -> (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        }
    }
}
