package com.spop.poverlay.launcher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.spop.poverlay.MainActivity
import com.spop.poverlay.overlay.OverlayService
import com.spop.poverlay.sensor.heartrate.HeartRateManager
import com.spop.poverlay.util.IsRunningOnPeloton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The bike's home screen (HOME intent). Starts and ends rides, and opens apps
 * with the ride overlay on top.
 */
class HomeActivity : ComponentActivity() {
    private val repository by lazy { AppRepository(this) }

    // Bumped on every resume so newly installed apps show up.
    private var refreshKey by mutableIntStateOf(0)

    // Set when opening our own settings screen, which minimizes the overlay itself.
    private var skipOverlayRestore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val rideActive by OverlayService.isRunning.collectAsState()
            val heartRateDevice by HeartRateManager.connectedDevice.collectAsState()
            var showAllApps by remember { mutableStateOf(false) }
            var pinned by remember { mutableStateOf<List<String>>(emptyList()) }

            val allApps by produceState(emptyList<LaunchableApp>(), refreshKey) {
                value = withContext(Dispatchers.IO) { repository.loadApps() }
                pinned = repository.pinnedPackages(value.map { it.packageName })
            }
            val greeting by produceState(greeting()) {
                while (true) {
                    value = greeting()
                    delay(15_000)
                }
            }
            LaunchedEffect(refreshKey) { showAllApps = false }

            val byPackage = allApps.associateBy { it.packageName }
            HomeScreen(
                state = HomeState(
                    greeting = greeting,
                    rideActive = rideActive,
                    bikeConnected = IsRunningOnPeloton,
                    heartRateDevice = heartRateDevice?.name,
                    pinnedApps = pinned.mapNotNull { byPackage[it] },
                    allApps = allApps,
                    pinnedPackages = pinned.toSet(),
                    showAllApps = showAllApps,
                ),
                actions = HomeActions(
                    onStartRide = ::startRide,
                    onEndRide = ::endRide,
                    onOpenApp = ::openApp,
                    onTogglePin = { pkg ->
                        pinned = when {
                            pkg in pinned -> pinned - pkg
                            pinned.size >= AppRepository.MaxPinned -> {
                                toast("Home screen is full. Hold a tile there to unpin it.")
                                pinned
                            }
                            else -> pinned + pkg
                        }
                        repository.setPinnedPackages(pinned)
                    },
                    onShowAllApps = { showAllApps = it },
                    onOpenOverlaySettings = {
                        skipOverlayRestore = true
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    onOpenSystemSettings = { startActivity(Intent(Settings.ACTION_SETTINGS)) },
                    onOpenPeloton = ::openPeloton,
                ),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey++
        // Keep the ride overlay out of the way while the home screen is showing.
        sendOverlayAction(OverlayService.ActionMinimizeOverlay)
    }

    override fun onStop() {
        super.onStop()
        // Leaving home for an app: bring the full overlay back.
        if (!skipOverlayRestore) {
            sendOverlayAction(OverlayService.ActionRestoreOverlay)
        }
        skipOverlayRestore = false
    }

    @Deprecated("Home screen has nowhere to go back to")
    override fun onBackPressed() {
        // Stay on the home screen.
    }

    private fun startRide() {
        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
        // Start collapsed so the full bar doesn't cover the home screen; it expands when an app opens.
        window.decorView.postDelayed({ sendOverlayAction(OverlayService.ActionMinimizeOverlay) }, 800)
    }

    private fun endRide() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun openApp(packageName: String) {
        val intent = repository.launchIntent(packageName)
        if (intent == null) {
            toast("Couldn't open that app")
            return
        }
        startActivity(intent)
    }

    private fun openPeloton() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(PelotonLauncherPackage, PelotonLauncherActivity))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast("Peloton's home screen isn't available")
        }
    }

    private fun sendOverlayAction(action: String) {
        if (OverlayService.isRunning.value) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java).setAction(action)
            )
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun greeting(): String {
        val now = Date()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val partOfDay = when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
        val day = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
        return "$day $partOfDay · $time"
    }

    companion object {
        const val PelotonLauncherPackage = "com.peloton.launcher"
        const val PelotonLauncherActivity = "com.peloton.launcher.LauncherActivity"
    }
}
