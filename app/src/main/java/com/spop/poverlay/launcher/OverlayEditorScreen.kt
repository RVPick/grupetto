package com.spop.poverlay.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spop.poverlay.overlay.BackgroundColorDefault
import com.spop.poverlay.overlay.MetricType
import com.spop.poverlay.overlay.OverlayCornerRadius
import com.spop.poverlay.overlay.OverlayItem
import com.spop.poverlay.overlay.OverlayLayout
import com.spop.poverlay.overlay.OverlayLocation
import com.spop.poverlay.overlay.OverlayService
import com.spop.poverlay.overlay.composables.OverlayMainContent
import com.spop.poverlay.overlay.composables.OverlayMinimizedContent
import com.spop.poverlay.ui.theme.PeloColors
import com.spop.poverlay.ui.theme.PeloFonts
import kotlin.math.sin

private val CardShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(16.dp)

@Composable
fun OverlayEditorScreen(
    layout: OverlayLayout,
    showTimerInPill: Boolean,
    onChange: ((OverlayLayout) -> OverlayLayout) -> Unit,
    onShowTimerInPill: (Boolean) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PeloColors.Woodsmoke)
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("OVERLAY", color = PeloColors.Text, fontFamily = PeloFonts.Numbers, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 40.sp)
                Text(
                    "Choose what your ride overlay shows. Changes apply right away, even mid-ride.",
                    color = PeloColors.TextMuted,
                    fontFamily = PeloFonts.Body,
                    fontSize = 15.sp
                )
            }
            Pill("Reset to default", onClick = onReset)
            Box(
                Modifier
                    .size(56.dp)
                    .background(PeloColors.Surface, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = PeloColors.Text, modifier = Modifier.size(28.dp))
            }
        }

        Preview(layout, showTimerInPill)

        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card("FULL BAR", Modifier.weight(1.2f)) { FullBarControls(layout, onChange) }
            Card("COMPACT PILL", Modifier.weight(1f)) { PillControls(layout, showTimerInPill, onChange, onShowTimerInPill) }
            Card("GENERAL", Modifier.weight(1f)) { GeneralControls(layout, onChange) }
        }
    }
}

/** The real overlay components with sample numbers, so the preview matches a ride exactly. */
@Composable
private fun Preview(layout: OverlayLayout, showTimerInPill: Boolean) {
    val sampleGraph = remember {
        List(160) { i -> 185f + 25f * sin(i * 0.35f) + 12f * sin(i * 1.3f) + if (i in 70..95) 80f * sin((i - 70) / 25f * Math.PI.toFloat()) else 0f }
    }
    val speedUnit = if (layout.useMph) "mph" else "km/h"
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0D10), CardShape)
            .border(1.dp, PeloColors.Divider, CardShape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .requiredHeight(OverlayService.OverlayHeightDp)
                .wrapContentWidth(unbounded = true)
                .background(BackgroundColorDefault, RoundedCornerShape(OverlayCornerRadius))
        ) {
            OverlayMainContent(
                modifier = Modifier
                    .wrapContentWidth(unbounded = true)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 6.dp),
                rowAlignment = Alignment.Bottom,
                power = "212",
                rpm = "88",
                currentGraph = sampleGraph,
                selectedMetric = MetricType.POWER,
                resistance = "41",
                speed = if (layout.useMph) "19.6" else "31.5",
                speedLabel = speedUnit,
                heartRate = "148",
                calories = "263",
                pauseChart = false,
                maxPower = "341",
                maxCadence = "102",
                maxResistance = "55",
                maxSpeed = if (layout.useMph) "24.1" else "38.8",
                maxPowerValue = 341f,
                maxCadenceValue = 102f,
                maxResistanceValue = 55f,
                maxSpeedValue = 24.1f,
                totalEnergy = "271",
                totalDistance = if (layout.useMph) "7.90" else "12.71",
                distanceUnit = if (layout.useMph) "mi" else "km",
                avgCadence = "84",
                avgResistance = "43",
                maxHeartRate = "171",
                avgHeartRate = "142",
                showHeartRateCard = true,
                layout = layout,
                onMetricSelected = {},
                onSpeedUnitClicked = {},
                onChartClicked = {},
            )
        }
        OverlayMinimizedContent(
            isMinimized = true,
            showTimerWhenMinimized = showTimerInPill,
            location = OverlayLocation.Bottom,
            powerLabel = "212",
            cadenceLabel = "88",
            resistanceLabel = "41",
            heartRateLabel = "148",
            speedLabel = if (layout.useMph) "19.6" else "31.5",
            speedUnit = speedUnit,
            caloriesLabel = "263",
            layout = layout,
            contentAlpha = 1f,
            timerLabel = "24:18",
            timerPaused = false,
            onTap = {},
            onLongPress = {},
            onOpenSettings = {},
            onMinimizeToggle = {},
            onLayout = {},
        )
    }
}

@Composable
private fun Card(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .background(PeloColors.Surface, CardShape)
            .border(1.dp, PeloColors.Divider, CardShape)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 2.sp)
        content()
    }
}

@Composable
private fun FullBarControls(layout: OverlayLayout, onChange: ((OverlayLayout) -> OverlayLayout) -> Unit) {
    layout.barOrder.forEachIndexed { index, item ->
        val shown = item !in layout.barHidden
        Row(
            Modifier
                .fillMaxWidth()
                .background(PeloColors.Woodsmoke, RowShape)
                .border(1.dp, PeloColors.Divider, RowShape)
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PeloSwitch(shown) { on ->
                onChange { l -> l.copy(barHidden = if (on) l.barHidden - item else l.barHidden + item) }
            }
            Text(
                item.label,
                color = if (shown) PeloColors.Text else PeloColors.TextMuted,
                fontFamily = PeloFonts.Body,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            ArrowButton(Icons.Filled.KeyboardArrowUp, "Move ${item.label} left", enabled = index > 0) {
                onChange { l -> l.copy(barOrder = l.barOrder.swap(index, index - 1)) }
            }
            ArrowButton(Icons.Filled.KeyboardArrowDown, "Move ${item.label} right", enabled = index < layout.barOrder.lastIndex) {
                onChange { l -> l.copy(barOrder = l.barOrder.swap(index, index + 1)) }
            }
        }
    }
    Hint("▲ moves an item left in the bar, ▼ moves it right. Heart rate only shows when a monitor is connected.")
    SwitchRow("Averages and max", "The small line under each number", layout.showStats) { on ->
        onChange { it.copy(showStats = on) }
    }
}

@Composable
private fun PillControls(
    layout: OverlayLayout,
    showTimerInPill: Boolean,
    onChange: ((OverlayLayout) -> OverlayLayout) -> Unit,
    onShowTimerInPill: (Boolean) -> Unit,
) {
    Hint("Pick up to ${OverlayLayout.MaxPillItems}, in the order you want them.")
    OverlayLayout.PillChoices.forEach { item ->
        val position = layout.pillItems.indexOf(item)
        val selected = position >= 0
        val full = !selected && layout.pillItems.size >= OverlayLayout.MaxPillItems
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(if (selected) PeloColors.Cardinal.copy(alpha = 0.18f) else PeloColors.Woodsmoke, RowShape)
                .border(1.dp, if (selected) PeloColors.Cardinal else PeloColors.Divider, RowShape)
                .clip(RowShape)
                .clickable(enabled = !full) {
                    onChange { l ->
                        l.copy(pillItems = if (item in l.pillItems) l.pillItems - item else l.pillItems + item)
                    }
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(if (selected) PeloColors.Cardinal else PeloColors.Surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Text("${position + 1}", color = Color.White, fontFamily = PeloFonts.Numbers, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
            Text(
                item.label,
                color = if (full) PeloColors.TextMuted else PeloColors.Text,
                fontFamily = PeloFonts.Body,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
    SwitchRow("Timer", "Show the ride timer in the pill", showTimerInPill, onShowTimerInPill)
}

@Composable
private fun GeneralControls(layout: OverlayLayout, onChange: ((OverlayLayout) -> OverlayLayout) -> Unit) {
    SwitchRow("Start collapsed", "Rides begin with the compact pill instead of the full bar", layout.startCollapsed) { on ->
        onChange { it.copy(startCollapsed = on) }
    }
    Label("Position")
    Segmented(
        options = listOf("Bottom" to OverlayLocation.Bottom, "Top" to OverlayLocation.Top),
        selected = layout.location,
    ) { loc -> onChange { it.copy(location = loc) } }
    Hint("You can still drag the overlay up or down during a ride.")
    Label("Units")
    Segmented(
        options = listOf("mph · mi" to true, "km/h · km" to false),
        selected = layout.useMph,
    ) { mph -> onChange { it.copy(useMph = mph) } }
}

@Composable
private fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(PeloColors.Woodsmoke, RowShape)
            .border(1.dp, PeloColors.Divider, RowShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) PeloColors.Cardinal else Color.Transparent, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else PeloColors.Pumice,
                    fontFamily = PeloFonts.Body,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PeloColors.Text, fontFamily = PeloFonts.Body, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(detail, color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 13.sp, lineHeight = 18.sp)
        }
        PeloSwitch(checked, onChange)
    }
}

@Composable
private fun PeloSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = PeloColors.Cardinal,
            checkedTrackAlpha = 1f,
            uncheckedThumbColor = PeloColors.Pumice,
            uncheckedTrackColor = PeloColors.BorderStrong,
            uncheckedTrackAlpha = 1f,
        )
    )
}

@Composable
private fun ArrowButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(if (enabled) PeloColors.Surface else Color.Transparent, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) PeloColors.Text else PeloColors.Divider, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(48.dp)
            .border(1.dp, PeloColors.BorderStrong, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = PeloColors.Text, fontFamily = PeloFonts.Body, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = PeloColors.Text, fontFamily = PeloFonts.Body, fontWeight = FontWeight.Medium, fontSize = 16.sp)
}

@Composable
private fun Hint(text: String) {
    Text(text, color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 13.sp, lineHeight = 18.sp)
}

private fun <T> List<T>.swap(i: Int, j: Int): List<T> =
    toMutableList().also { val tmp = it[i]; it[i] = it[j]; it[j] = tmp }
