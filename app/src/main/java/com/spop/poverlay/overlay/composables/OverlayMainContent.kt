package com.spop.poverlay.overlay.composables

// import androidx.compose.material.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.spop.poverlay.overlay.MetricType
import com.spop.poverlay.overlay.PowerChartFullWidth
import com.spop.poverlay.overlay.PowerChartShrunkWidth
import com.spop.poverlay.overlay.StatCard
import com.spop.poverlay.overlay.StatCardWidth
import com.spop.poverlay.ui.theme.MetricCadenceColor
import com.spop.poverlay.ui.theme.MetricCalorieColor
import com.spop.poverlay.ui.theme.MetricHeartRateColor
import com.spop.poverlay.ui.theme.MetricPowerColor
import com.spop.poverlay.ui.theme.MetricResistanceColor
import com.spop.poverlay.ui.theme.MetricSpeedColor
import com.spop.poverlay.util.LineChart

@Composable
fun OverlayMainContent(
        modifier: Modifier,
        rowAlignment: Alignment.Vertical,
        power: String,
        rpm: String,
        currentGraph: List<Float>,
        selectedMetric: MetricType,
        resistance: String,
        speed: String,
        speedLabel: String,
        heartRate: String,
        calories: String,
        pauseChart: Boolean,
        maxPower: String,
        maxCadence: String,
        maxResistance: String,
        maxSpeed: String,
        maxPowerValue: Float,
        maxCadenceValue: Float,
        maxResistanceValue: Float,
        maxSpeedValue: Float,
        totalEnergy: String,
        totalDistance: String,
        distanceUnit: String,
        avgCadence: String,
        avgResistance: String,
        maxHeartRate: String,
        avgHeartRate: String,
        showHeartRateCard: Boolean,
        onMetricSelected: (MetricType) -> Unit,
        onSpeedUnitClicked: () -> Unit,
        onChartClicked: () -> Unit
) {
    var shrinkChart by remember { mutableStateOf(false) }

    val chartColor =
            when (selectedMetric) {
                MetricType.POWER -> MetricPowerColor
                MetricType.CADENCE -> MetricCadenceColor
                MetricType.RESISTANCE -> MetricResistanceColor
                MetricType.SPEED -> MetricSpeedColor
                MetricType.HEART_RATE -> MetricHeartRateColor
            }

    // Define minimum thresholds to prevent chart from getting too compressed at low values
    // Use session max if higher than threshold, otherwise use threshold
    val chartMaxValue =
            when (selectedMetric) {
                MetricType.POWER -> maxOf(250f, maxPowerValue)
                MetricType.CADENCE -> maxOf(160f, maxCadenceValue)
                MetricType.RESISTANCE -> maxOf(100f, maxResistanceValue)
                MetricType.SPEED -> maxOf(40f, maxSpeedValue)
                MetricType.HEART_RATE -> 220f
            }

    Row(
            modifier = modifier,
            verticalAlignment = rowAlignment,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val statCardModifier = Modifier.requiredWidth(StatCardWidth)

        StatCard(
                name = "Output",
                value = power,
                unit = "W",
                modifier = statCardModifier,
                detail = stats("$totalEnergy kJ", maxPower),
                color = MetricPowerColor,
                selected = selectedMetric == MetricType.POWER,
                onClick = { onMetricSelected(MetricType.POWER) }
        )

        StatCard(
                name = "Cadence",
                value = rpm,
                unit = "rpm",
                modifier = statCardModifier,
                detail = stats("avg $avgCadence", maxCadence),
                color = MetricCadenceColor,
                selected = selectedMetric == MetricType.CADENCE,
                onClick = { onMetricSelected(MetricType.CADENCE) }
        )

        val chartWidth =
                if (shrinkChart) {
                    PowerChartShrunkWidth
                } else {
                    PowerChartFullWidth
                }
        val chartPadding =
                if (shrinkChart) {
                    15.dp
                } else {
                    8.dp
                }

        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                    onTap = { onChartClicked() },
                                    onLongPress = { shrinkChart = !shrinkChart }
                            )
                        }
        ) {
            /* Text(
                text = chartLabel,
                color = chartColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )*/
            LineChart(
                    data = currentGraph,
                    maxValue = chartMaxValue,
                    pauseChart = pauseChart,
                    modifier =
                            Modifier.requiredWidth(chartWidth)
                                    .requiredHeight(90.dp)
                                    .padding(horizontal = chartPadding),
                    fillColor = chartColor.copy(alpha = 0.25f),
                    lineColor = chartColor,
            )
        }

        StatCard(
                name = "Resistance",
                value = resistance,
                unit = "%",
                modifier = statCardModifier,
                detail = stats("avg $avgResistance", maxResistance),
                color = MetricResistanceColor,
                selected = selectedMetric == MetricType.RESISTANCE,
                onClick = { onMetricSelected(MetricType.RESISTANCE) }
        )

        StatCard(
                name = "Speed",
                value = speed,
                unit = speedLabel,
                modifier = statCardModifier,
                detail = stats("$totalDistance $distanceUnit", maxSpeed),
                color = MetricSpeedColor,
                selected = selectedMetric == MetricType.SPEED,
                onClick = { onMetricSelected(MetricType.SPEED) },
                onUnitClick = onSpeedUnitClicked
        )

        if (showHeartRateCard) {
                StatCard(
                        name = "Heart Rate",
                        value = heartRate,
                        unit = "bpm",
                        modifier = statCardModifier,
                        detail = stats("avg $avgHeartRate", maxHeartRate),
                        color = MetricHeartRateColor,
                        selected = selectedMetric == MetricType.HEART_RATE,
                        onClick = { onMetricSelected(MetricType.HEART_RATE) }
                )
        }

        StatCard(
                name = "Calories",
                value = calories,
                unit = "kcal",
                modifier = statCardModifier,
                color = MetricCalorieColor
        )
    }
}

/** "avg 84 · max 110", leaving out the max until one has been recorded. */
private fun stats(first: String, max: String) =
        if (max == "0" || max == "0.0") first else "$first · max $max"
