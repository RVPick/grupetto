package com.spop.poverlay.launcher

import android.app.ActivityManager
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
import androidx.lifecycle.lifecycleScope
import com.spop.poverlay.MainActivity
import com.spop.poverlay.overlay.OverlayService
import com.spop.poverlay.sensor.heartrate.HeartRateManager
import com.spop.poverlay.util.IsRunningOnPeloton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // Shown on the Close apps button while closing and briefly afterwards (a toast would sit under the ride pill).
    private var closeAppsStatus by mutableStateOf<String?>(null)

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
                    closeAppsStatus = closeAppsStatus,
                ),
                actions = HomeActions(
                    onStartRide = ::openDashboard,
                    onOpenDashboard = ::openDashboard,
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
                    onCloseApps = ::closeBackgroundApps,
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

    /** Opens the ride dashboard, which starts a ride if none is running. */
    private fun openDashboard() {
        startActivity(Intent(this, RideActivity::class.java))
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

    /**
     * Closes every background app we launch (not Pelo or its ride overlay, and not
     * Peloton's system apps). Only works on apps that aren't in the foreground, which
     * while the home screen is showing is all of them.
     */
    private fun closeBackgroundApps() {
        if (closeAppsStatus != null) return
        val activityManager = getSystemService(ActivityManager::class.java)
        closeAppsStatus = ClosingLabel
        lifecycleScope.launch {
            val freeMb = withContext(Dispatchers.IO) {
                repository.launchablePackages().forEach(activityManager::killBackgroundProcesses)
                // Apps exit asynchronously; give the system a moment before measuring.
                delay(3_000)
                availableMemoryMb(activityManager)
            }
            closeAppsStatus = "$freeMb MB free"
            delay(4_000)
            closeAppsStatus = null
        }
    }

    private fun availableMemoryMb(activityManager: ActivityManager): Long {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return info.availMem / (1024 * 1024)
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
