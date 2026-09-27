package com.spop.poverlay.launcher

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

/**
 * Installed apps the home screen can open, plus the user's pinned list
 * (stored in order, as package names).
 */
class AppRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Every launchable app except this one and Peloton's own packages. Slow: call off the main thread. */
    fun loadApps(): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName && !isPelotonPackage(it.packageName) }
            .distinctBy { it.packageName }
            .map { info ->
                LaunchableApp(
                    packageName = info.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(IconSizePx, IconSizePx).asImageBitmap(),
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

    fun launchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    private fun isPelotonPackage(packageName: String) =
        packageName.startsWith("com.peloton") || packageName.startsWith("com.onepeloton")

    companion object {
        private const val KeyPinned = "pinned"
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
