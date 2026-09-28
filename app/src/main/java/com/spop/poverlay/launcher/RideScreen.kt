package com.spop.poverlay.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spop.poverlay.ui.theme.PeloColors
import com.spop.poverlay.ui.theme.PeloFonts
import com.spop.poverlay.util.LineChart

private val CardShape = RoundedCornerShape(24.dp)
private val ButtonShape = RoundedCornerShape(16.dp)

/** Everything the dashboard shows, already formatted. */
data class RideState(
    val timer: String,
    val timerPaused: Boolean,
    val timerStarted: Boolean,
    val power: String,
    val maxPower: String,
    val cadence: String,
    val avgCadence: String,
    val resistance: String,
    val resistanceFraction: Float,
    val heartRate: String?,
    val avgHeartRate: String,
    val maxHeartRate: String,
    /** 1–5 when HR zones are configured and a reading is available. */
    val heartRateZone: Int?,
    val outputKj: String,
    val distance: String,
    val distanceUnit: String,
    val speed: String,
    val speedUnit: String,
    val calories: String,
    val powerGraph: List<Float>,
    val powerGraphMax: Float,
    /** Null while the first load is still running. */
    val pinnedApps: List<LaunchableApp>?,
    val showAppPicker: Boolean,
)

class RideActions(
    val onPauseToggle: () -> Unit,
    val onWatchSomething: () -> Unit,
    val onDismissAppPicker: () -> Unit,
    val onOpenApp: (String) -> Unit,
    val onEndRide: () -> Unit,
)

@Composable
fun RideScreen(state: RideState?, actions: RideActions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PeloColors.Woodsmoke)
    ) {
        if (state == null) {
            Text(
                "Starting ride…",
                color = PeloColors.TextMuted,
                fontFamily = PeloFonts.Body,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Header(state)
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(0.48f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MetricCard("OUTPUT", state.power, "W", "max ${state.maxPower} W", PeloColors.CardinalBright, Modifier.weight(1f))
                        MetricCard("CADENCE", state.cadence, "rpm", "avg ${state.avgCadence}", PeloColors.Text, Modifier.weight(1f))
                        MetricCard("RESISTANCE", state.resistance, "%", null, PeloColors.Text, Modifier.weight(1f)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(PeloColors.Divider)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(state.resistanceFraction.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(PeloColors.Cardinal, CircleShape)
                                )
                            }
                        }
                    }
                    PowerChartCard(state, Modifier.weight(0.52f))
                }
                Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    HeartRateCard(state, Modifier.weight(0.48f))
                    TotalsCard(state, Modifier.weight(0.52f))
                }
            }
            BottomButtons(state, actions)
        }
        if (state.showAppPicker) {
            AppPicker(state.pinnedApps, actions)
        }
    }
}

@Composable
private fun Header(state: RideState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label("JUST RIDE", PeloColors.CardinalBright)
            Text(
                when {
                    !state.timerStarted -> "Start pedaling and the clock starts"
                    state.timerPaused -> "Paused"
                    else -> "Free ride · pauses when you stop"
                },
                color = PeloColors.TextMuted,
                fontFamily = PeloFonts.Body,
                fontSize = 15.sp
            )
        }
        // Right-aligned: Peloton's own banner sits over the top center of the screen.
        Text(
            state.timer,
            color = if (state.timerPaused && state.timerStarted) PeloColors.TextMuted else PeloColors.Text,
            fontFamily = PeloFonts.Numbers,
            fontWeight = FontWeight.SemiBold,
            fontSize = 72.sp,
            lineHeight = 72.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun Label(text: String, color: Color = PeloColors.TextMuted) {
    Text(
        text,
        color = color,
        fontFamily = PeloFonts.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 2.sp
    )
}

@Composable
private fun Card(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PeloColors.Surface, CardShape)
            .border(1.dp, PeloColors.Divider, CardShape)
            .padding(horizontal = 26.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        content()
    }
}

@Composable
private fun BigNumber(value: String, unit: String, color: Color, size: TextUnit = 112.sp) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            value,
            color = color,
            fontFamily = PeloFonts.Numbers,
            fontWeight = FontWeight.Bold,
            fontSize = size,
            lineHeight = size,
            maxLines = 1,
            modifier = Modifier.alignByBaseline()
        )
        Text(
            unit,
            color = PeloColors.TextMuted,
            fontFamily = PeloFonts.Body,
            fontSize = 20.sp,
            modifier = Modifier.alignByBaseline()
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    detail: String?,
    color: Color,
    modifier: Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    Card(modifier) {
        Label(label)
        BigNumber(value, unit, color)
        if (footer != null) {
            footer()
        } else {
            Text(detail ?: "", color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PowerChartCard(state: RideState, modifier: Modifier) {
    Card(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label("OUTPUT · LAST 2½ MIN")
            Text("max ${state.maxPower} W", color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        LineChart(
            data = state.powerGraph,
            maxValue = state.powerGraphMax,
            pauseChart = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            fillColor = PeloColors.Cardinal.copy(alpha = 0.25f),
            lineColor = PeloColors.CardinalBright,
        )
    }
}

@Composable
private fun HeartRateCard(state: RideState, modifier: Modifier) {
    Card(modifier) {
        Label("HEART RATE")
        if (state.heartRate == null) {
            Text(
                "No strap connected",
                color = PeloColors.Pumice,
                fontFamily = PeloFonts.Body,
                fontSize = 20.sp
            )
            Text(
                "Pair one in Overlay settings",
                color = PeloColors.TextMuted,
                fontFamily = PeloFonts.Body,
                fontSize = 14.sp
            )
        } else {
            BigNumber(state.heartRate, "bpm", PeloColors.Text, size = 96.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.heartRateZone?.let { zone ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (z in 1..5) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .background(
                                        if (z == zone) PeloColors.CardinalBright else PeloColors.Divider,
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                }
                Text(
                    listOfNotNull(
                        state.heartRateZone?.let { "Zone $it of 5" },
                        "avg ${state.avgHeartRate}",
                        "max ${state.maxHeartRate}"
                    ).joinToString(" · "),
                    color = PeloColors.TextMuted,
                    fontFamily = PeloFonts.Body,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TotalsCard(state: RideState, modifier: Modifier) {
    Card(modifier) {
        TotalRow("Output", "${state.outputKj} kJ")
        Divider()
        TotalRow("Distance", "${state.distance} ${state.distanceUnit}")
        Divider()
        TotalRow("Speed", "${state.speed} ${state.speedUnit}")
        Divider()
        TotalRow("Calories", "${state.calories} kcal")
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 15.sp)
        Text(
            value,
            color = PeloColors.Text,
            fontFamily = PeloFonts.Numbers,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp
        )
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PeloColors.Divider)
    )
}

@Composable
private fun BottomButtons(state: RideState, actions: RideActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RideButton(
            if (state.timerPaused && state.timerStarted) "Resume" else "Pause",
            Modifier.width(180.dp),
            enabled = state.timerStarted,
            onClick = actions.onPauseToggle
        )
        RideButton(
            "Watch something · keep metrics on top",
            Modifier.weight(1f),
            icon = true,
            onClick = actions.onWatchSomething
        )
        RideButton(
            "End ride",
            Modifier.width(180.dp),
            background = PeloColors.Pumice,
            content = PeloColors.Woodsmoke,
            onClick = actions.onEndRide
        )
    }
}

@Composable
private fun RideButton(
    label: String,
    modifier: Modifier,
    background: Color = PeloColors.Surface,
    content: Color = PeloColors.Text,
    enabled: Boolean = true,
    icon: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxHeight()
            .background(background, ButtonShape)
            .border(1.dp, if (background == PeloColors.Surface) PeloColors.Divider else background, ButtonShape)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = if (enabled) content else PeloColors.TextMuted,
            fontFamily = PeloFonts.Body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun AppPicker(apps: List<LaunchableApp>?, actions: RideActions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { actions.onDismissAppPicker() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(880.dp)
                .background(PeloColors.Surface, CardShape)
                .border(1.dp, PeloColors.Divider, CardShape)
                .consumeTaps() // taps on the panel shouldn't reach the background and close it
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Label("WATCH SOMETHING", PeloColors.CardinalBright)
                    Text(
                        "Your metrics stay on top of the app.",
                        color = PeloColors.TextMuted,
                        fontFamily = PeloFonts.Body,
                        fontSize = 15.sp
                    )
                }
                Box(
                    Modifier
                        .size(56.dp)
                        .background(PeloColors.Woodsmoke, CircleShape)
                        .clickable { actions.onDismissAppPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = PeloColors.Text, modifier = Modifier.size(28.dp))
                }
            }
            if (apps == null) {
                Text(
                    "Loading your apps…",
                    color = PeloColors.Pumice,
                    fontFamily = PeloFonts.Body,
                    fontSize = 17.sp
                )
            } else if (apps.isEmpty()) {
                Text(
                    "No pinned apps yet. Pin some from All apps on the home screen.",
                    color = PeloColors.Pumice,
                    fontFamily = PeloFonts.Body,
                    fontSize = 17.sp
                )
            } else {
                apps.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { app ->
                            Row(
                                Modifier
                                    .weight(1f)
                                    .height(96.dp)
                                    .background(PeloColors.Woodsmoke, RoundedCornerShape(20.dp))
                                    .border(1.dp, PeloColors.Divider, RoundedCornerShape(20.dp))
                                    .clickable { actions.onOpenApp(app.packageName) }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Image(app.icon, contentDescription = null, modifier = Modifier.size(52.dp))
                                Text(
                                    app.label,
                                    color = PeloColors.Text,
                                    fontFamily = PeloFonts.Body,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
