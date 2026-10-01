package com.spop.poverlay.launcher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.spop.poverlay.overlay.OverlayService
import com.spop.poverlay.overlay.RideSession
import com.spop.poverlay.overlay.SensorValuePlaceholderText
import com.spop.poverlay.sensor.heartrate.HeartRateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen ride dashboard. Starts a ride if none is running, and hides the
 * overlay while it's showing since it displays the same metrics.
 */
class RideActivity : ComponentActivity() {
    private val repository by lazy { AppRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!OverlayService.isRunning.value) {
            ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
        }
        // A ride started from here doesn't exist yet at onResume, so (re)hide the overlay
        // whenever a session appears while the dashboard is in front.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                OverlayService.session.filterNotNull().collect {
                    sendOverlayAction(OverlayService.ActionHideOverlay)
                }
            }
        }

        setContent {
            val session by OverlayService.session.collectAsState()
            var showAppPicker by remember { mutableStateOf(false) }
            // Load pinned apps as soon as the dashboard opens (loading every icon takes a moment
            // on the bike), and refresh each time the picker opens, keeping the last list meanwhile.
            // Null means the first load hasn't finished.
            val pinnedApps by produceState<List<LaunchableApp>?>(null, showAppPicker) {
                if (value != null && !showAppPicker) return@produceState // no reload when it closes
                value = withContext(Dispatchers.IO) {
                    // Only the pinned apps' icons are needed here.
                    val pinned = repository.pinnedPackages(repository.launchablePackages())
                    val byPackage = repository.loadApps(only = pinned).associateBy { it.packageName }
                    pinned.mapNotNull { byPackage[it] }
                }
            }

            val actions = RideActions(
                onPauseToggle = { session?.timer?.onTimerTap() },
                onWatchSomething = { showAppPicker = true },
                onDismissAppPicker = { showAppPicker = false },
                onOpenApp = { pkg ->
                    showAppPicker = false
                    repository.launchIntent(pkg)?.let(::startActivity)
                },
                onEndRide = {
                    stopService(Intent(this, OverlayService::class.java))
                    finish()
                },
            )
            RideScreen(session?.let { rideState(it, pinnedApps, showAppPicker) }, actions)
        }
    }

    override fun onStop() {
        super.onStop()
        sendOverlayAction(OverlayService.ActionShowOverlay)
    }

    private fun sendOverlayAction(action: String) {
        if (OverlayService.isRunning.value) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java).setAction(action)
            )
        }
    }
}

@Composable
private fun rideState(session: RideSession, pinnedApps: List<LaunchableApp>?, showAppPicker: Boolean): RideState {
    val sensor = session.sensor
    val timer = session.timer
    val timerLabel by timer.timerLabel.collectAsState()
    val timerPaused by timer.timerPaused.collectAsState()
    val power by sensor.powerValue.collectAsState(initial = SensorValuePlaceholderText)
    val cadence by sensor.rpmValue.collectAsState(initial = SensorValuePlaceholderText)
    val resistance by sensor.resistanceValue.collectAsState(initial = SensorValuePlaceholderText)
    val speed by sensor.speedValue.collectAsState(initial = SensorValuePlaceholderText)
    val speedUnit by sensor.speedLabel.collectAsState(initial = "mph")
    val calories by sensor.caloriesValue.collectAsState(initial = SensorValuePlaceholderText)
    val maxPower by sensor.maxPower.collectAsState()
    val avgCadence by sensor.avgCadence.collectAsState()
    val totalEnergy by sensor.totalEnergy.collectAsState()
    val totalDistanceMiles by sensor.totalDistance.collectAsState()
    val heartRate by HeartRateManager.heartRate.collectAsState()
    val heartRateDevice by HeartRateManager.connectedDevice.collectAsState()
    val avgHeartRate by sensor.avgHeartRate.collectAsState()
    val maxHeartRate by sensor.maxHeartRate.collectAsState()
    val zoneThresholds = listOf(
        HeartRateManager.zone12.collectAsState().value,
        HeartRateManager.zone23.collectAsState().value,
        HeartRateManager.zone34.collectAsState().value,
        HeartRateManager.zone45.collectAsState().value,
    )

    val metric = speedUnit != "mph"
    val hr = heartRate.takeIf { heartRateDevice != null }
    return RideState(
        timer = timerLabel,
        timerPaused = timerPaused,
        timerStarted = timerLabel.any { it.isDigit() },
        power = power,
        maxPower = "%.0f".format(maxPower),
        cadence = cadence,
        avgCadence = "%.0f".format(avgCadence),
        resistance = resistance,
        resistanceFraction = (resistance.toFloatOrNull() ?: 0f) / 100f,
        heartRate = hr?.toString(),
        avgHeartRate = "%.0f".format(avgHeartRate),
        maxHeartRate = "%.0f".format(maxHeartRate),
        heartRateZone = if (hr != null && zoneThresholds.all { it != null }) {
            1 + zoneThresholds.count { hr >= it!! }
        } else {
            null
        },
        outputKj = "%.0f".format(totalEnergy),
        distance = "%.2f".format(if (metric) totalDistanceMiles * 1.60934f else totalDistanceMiles),
        distanceUnit = if (metric) "km" else "mi",
        speed = speed,
        speedUnit = speedUnit,
        calories = calories,
        powerGraph = sensor.powerGraph,
        powerGraphMax = maxOf(250f, maxPower),
        pinnedApps = pinnedApps,
        showAppPicker = showAppPicker,
    )
}
