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

        private val DefaultPins = listOf(
            "org.schabi.newpipe",
            "com.netflix.mediaclient",
            "com.google.android.youtube",
            "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient",
            "com.android.chrome",
        )
    }
}
