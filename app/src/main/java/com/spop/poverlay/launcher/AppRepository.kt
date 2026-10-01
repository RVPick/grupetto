package com.spop.poverlay.launcher

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    /** False for apps that came with the tablet or that Peloton installed (e.g. Netflix). */
    val canUninstall: Boolean = true,
)

/** An app opened since boot that hasn't been closed from Pelo since. */
data class RunningApp(
    val app: LaunchableApp,
    val lastUsedMillis: Long,
)

/**
 * Installed apps the home screen can open, plus the user's pinned list
 * (stored in order, as package names).
 */
class AppRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /**
     * Every launchable app except this one and Peloton's own packages, or just [only] when given
     * (icons are the slow part, so load only what's needed). Slow: call off the main thread.
     */
    fun loadApps(only: Collection<String>? = null): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName && !isPelotonPackage(it.packageName) }
            .filter { only == null || it.packageName in only }
            .distinctBy { it.packageName }
            .map { info ->
                LaunchableApp(
                    packageName = info.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(IconSizePx, IconSizePx).asImageBitmap(),
                    canUninstall = canUninstall(info.applicationInfo),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Package names of every app [loadApps] would return, without loading icons. */
    fun launchablePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName && !isPelotonPackage(it) }
            .toSet()
    }

    fun pinnedPackages(installed: Collection<String>): List<String> {
        val stored = prefs.getString(KeyPinned, null)
            ?: return DefaultPins.filter { it in installed }
        return stored.split(',').filter { it in installed }
    }

    fun setPinnedPackages(packages: List<String>) {
        prefs.edit().putString(KeyPinned, packages.joinToString(",")).apply()
    }

    /** Whether Pelo can read app usage (granted over ADB by scripts/grant.sh). */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        return appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Apps opened since boot and not closed from Pelo since, most recently used first,
     * as package name to last-used time. Like Android's recents screen, an app the
     * system has already closed to free memory can still be listed.
     * Returns null without usage access.
     */
    fun runningPackages(): List<Pair<String, Long>>? {
        if (!hasUsageAccess()) return null
        val usageStats = context.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val bootTime = now - SystemClock.elapsedRealtime()
        val lastResumed = mutableMapOf<String, Long>()
        val events = usageStats.queryEvents(bootTime, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumed[event.packageName] = event.timeStamp
            }
        }
        val launchable = launchablePackages()
        val apps = lastResumed.filterKeys { it in launchable }
        // Peloton's screens (launcher, activation/classes app) show up as one "Peloton" entry.
        val pelotonLastUsed = lastResumed.filterKeys { it in PelotonScreenPackages }.values.maxOrNull()
        return (apps + listOfNotNull(pelotonLastUsed?.let { PelotonKey to it }))
            .filter { (key, lastUsed) -> lastUsed > prefs.getLong(KeyClosedPrefix + key, 0L) }
            .toList()
            .sortedByDescending { it.second }
    }

    /** Whether opening an app from Pelo first closes the other background apps. */
    var closeOthersOnOpen: Boolean
        get() = prefs.getBoolean(KeyCloseOthers, true)
        set(value) = prefs.edit().putBoolean(KeyCloseOthers, value).apply()

    /**
     * Closes background apps Pelo can launch, except [except] (Pelo, its ride overlay and
     * Peloton's apps are never included). Apps on screen, like a mini player, aren't affected.
     */
    fun closeApps(except: String? = null) {
        close(launchablePackages() + PelotonKey - setOfNotNull(except))
    }

    /**
     * Closes the given apps if they're in the background. [PelotonKey] stands for Peloton's
     * screens only ([PelotonScreenPackages]); Peloton's sensor services, keyboard and
     * background services are never closed (Pelo reads the bike through the sensor services).
     */
    fun close(keys: Collection<String>) {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        keys.flatMap { if (it == PelotonKey) PelotonScreenPackages else listOf(it) }
            .forEach(activityManager::killBackgroundProcesses)
        markClosed(keys)
    }

    fun markClosed(packages: Collection<String>) {
        val now = System.currentTimeMillis()
        prefs.edit().apply { packages.forEach { putLong(KeyClosedPrefix + it, now) } }.apply()
    }

    private fun canUninstall(info: ApplicationInfo): Boolean {
        val isSystem = info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (isSystem) return false
        // Peloton's device manager may reinstall apps it put there, so leave those alone.
        val installer = try {
            context.packageManager.getInstallSourceInfo(info.packageName).installingPackageName
        } catch (e: Exception) {
            null
        }
        return installer == null || !isPelotonPackage(installer)
    }

    /** The Peloton logo, taken from Peloton's own app on the tablet (null if it's missing). */
    private val cachedPelotonLogo by lazy { loadPelotonLogo() }
    fun pelotonLogo(): ImageBitmap? = cachedPelotonLogo

    /** Peloton's screens as a running-apps entry, keyed by [PelotonKey]. */
    fun pelotonApp(): LaunchableApp? = pelotonLogo()?.let { LaunchableApp(PelotonKey, "Peloton", it, canUninstall = false) }

    private fun loadPelotonLogo(): ImageBitmap? = try {
        context.packageManager.getApplicationIcon(PelotonLogoPackage)
            .toBitmap(IconSizePx, IconSizePx).asImageBitmap()
    } catch (e: Exception) {
        null
    }

    /** Opens Android's own "uninstall this app?" confirmation. */
    fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))

    fun launchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    private fun isPelotonPackage(packageName: String) =
        packageName.startsWith("com.peloton") || packageName.startsWith("com.onepeloton")

    companion object {
        private const val KeyPinned = "pinned"
        private const val KeyClosedPrefix = "closed_"
        private const val KeyCloseOthers = "close_others_on_open"
        private const val PelotonLogoPackage = "com.peloton.activity"

        /** Stands for Peloton's screens in the running-apps list and when closing apps. */
        const val PelotonKey = "peloton"

        /**
         * Peloton's screens: started by the Peloton button and safe to close like any app.
         * Never include com.peloton.service.SensorData or com.onepeloton.affernetservice.
         */
        val PelotonScreenPackages = listOf(
            "com.peloton.launcher",
            "com.peloton.activity",
            "com.onepeloton.workoutservices.app",
        )
        private const val IconSizePx = 144
        const val MaxPinned = 6

        // Streaming apps that work on the bike without Google Play Services.
        private val DefaultPins = listOf(
            "com.netflix.mediaclient",
            "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient", // Prime Video
            "com.peacocktv.peacockandroid",
            "org.smarttube.stable", // YouTube (the official app needs Play Services)
            "com.github.andreyasadchy.xtra", // Twitch (Google Play won't offer the official app to this device)
        )
    }
}
