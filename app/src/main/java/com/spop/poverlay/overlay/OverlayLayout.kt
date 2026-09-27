package com.spop.poverlay.overlay

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Things the overlay can show. [Chart] only appears in the full bar. */
enum class OverlayItem(val label: String) {
    Output("Output"),
    Cadence("Cadence"),
    Chart("Chart"),
    Resistance("Resistance"),
    Speed("Speed"),
    HeartRate("Heart rate"),
    Calories("Calories"),
}

/** How the ride overlay is laid out, edited on Pelo's Overlay page. */
data class OverlayLayout(
    /** Every full-bar item in display order, including hidden ones (so they keep their place). */
    val barOrder: List<OverlayItem> = DefaultBarOrder,
    val barHidden: Set<OverlayItem> = emptySet(),
    /** Show the "avg · max" line under each number in the full bar. */
    val showStats: Boolean = true,
    /** Metrics in the compact pill, in order (at most [MaxPillItems]). */
    val pillItems: List<OverlayItem> = DefaultPillItems,
    val startCollapsed: Boolean = true,
    val location: OverlayLocation = OverlayLocation.Bottom,
    val useMph: Boolean = true,
) {
    val barItems: List<OverlayItem> get() = barOrder.filter { it !in barHidden }

    companion object {
        const val MaxPillItems = 4
        val DefaultBarOrder = listOf(
            OverlayItem.Output,
            OverlayItem.Cadence,
            OverlayItem.Chart,
            OverlayItem.Resistance,
            OverlayItem.Speed,
            OverlayItem.HeartRate,
            OverlayItem.Calories,
        )
        val DefaultPillItems = listOf(
            OverlayItem.Output,
            OverlayItem.Cadence,
            OverlayItem.Resistance,
            OverlayItem.HeartRate,
        )
        val PillChoices = OverlayItem.values().filter { it != OverlayItem.Chart }
    }
}

/**
 * The saved [OverlayLayout], shared in-process by the Overlay page and the running
 * overlay so edits apply immediately, even mid-ride. Call [init] once at startup.
 */
object OverlayLayoutStore {
    private const val PrefsName = "overlay_layout"
    private val mutableLayout = MutableStateFlow(OverlayLayout())
    val layout: StateFlow<OverlayLayout> = mutableLayout.asStateFlow()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        prefs = p
        val defaults = OverlayLayout()
        mutableLayout.value = OverlayLayout(
            barOrder = p.getString("bar_order", null)?.let(::parseItems)
                ?.let { saved -> saved + (defaults.barOrder - saved.toSet()) } // add items new in this version
                ?: defaults.barOrder,
            barHidden = p.getString("bar_hidden", null)?.let(::parseItems)?.toSet() ?: defaults.barHidden,
            showStats = p.getBoolean("show_stats", defaults.showStats),
            pillItems = p.getString("pill_items", null)?.let(::parseItems)
                ?.filter { it != OverlayItem.Chart }?.take(OverlayLayout.MaxPillItems)
                ?: defaults.pillItems,
            startCollapsed = p.getBoolean("start_collapsed", defaults.startCollapsed),
            location = p.getString("location", null)
                ?.let { runCatching { OverlayLocation.valueOf(it) }.getOrNull() } ?: defaults.location,
            useMph = p.getBoolean("use_mph", defaults.useMph),
        )
    }

    fun update(transform: (OverlayLayout) -> OverlayLayout) {
        val updated = transform(mutableLayout.value)
        mutableLayout.value = updated
        prefs?.edit()?.apply {
            putString("bar_order", updated.barOrder.joinToString(",") { it.name })
            putString("bar_hidden", updated.barHidden.joinToString(",") { it.name })
            putBoolean("show_stats", updated.showStats)
            putString("pill_items", updated.pillItems.joinToString(",") { it.name })
            putBoolean("start_collapsed", updated.startCollapsed)
            putString("location", updated.location.name)
            putBoolean("use_mph", updated.useMph)
        }?.apply()
    }

    private fun parseItems(value: String): List<OverlayItem> =
        value.split(',').mapNotNull { name -> OverlayItem.values().firstOrNull { it.name == name } }.distinct()
}
