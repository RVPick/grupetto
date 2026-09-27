package com.spop.poverlay.overlay.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import com.spop.poverlay.ui.theme.MetricCadenceColor
import com.spop.poverlay.ui.theme.MetricHeartRateColor
import com.spop.poverlay.ui.theme.MetricPowerColor
import com.spop.poverlay.ui.theme.MetricResistanceColor
import com.spop.poverlay.ui.theme.PeloColors
import com.spop.poverlay.ui.theme.PeloFonts
import com.spop.poverlay.overlay.BackgroundColorDefault
import com.spop.poverlay.overlay.OverlayLocation


/** Mid-ride touch targets: big enough to hit while pedaling. */
private val ToggleButtonSize = 56.dp
private val SettingsButtonSize = 44.dp

@Composable
fun OverlayMinimizedContent(
    isMinimized: Boolean,
    showTimerWhenMinimized: Boolean,
    location: OverlayLocation,
    powerLabel: String,
    cadenceLabel: String,
    resistanceLabel: String,
    heartRateLabel: String,
    contentAlpha: Float,
    timerLabel: String,
    timerPaused: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onOpenSettings: () -> Unit,
    onMinimizeToggle: () -> Unit,
    onLayout: (IntSize) -> Unit
) {
    val backgroundShape = if (isMinimized) {
        RoundedCornerShape(percent = 50)
    } else {
        when (location) {
            OverlayLocation.Top -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            OverlayLocation.Bottom -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        }
    }
    val size = remember { mutableStateOf(IntSize.Zero) }

    Row(
        modifier = Modifier
            .alpha(contentAlpha)
            .wrapContentSize().onSizeChanged {
                if (it.width != size.value.width || it.height != size.value.height) {
                    size.value = it
                    onLayout(size.value)
                }
            }
            .padding(vertical = if (isMinimized) 4.dp else 0.dp)
            .background(
                color = BackgroundColorDefault,
                shape = backgroundShape,
            )
            .padding(start = 22.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .animateContentSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTap()
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
            },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        // The "Timer in compact mode" switch decides, even while paused (Grupetto used to
        // always show a paused timer, which made the switch look broken off the bike).
        if (!isMinimized || showTimerWhenMinimized) {

            val timerAlpha = if (timerPaused) {
                infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                ).value
            } else {
                1f
            }

            Text(
                timerLabel,
                color = PeloColors.Text,
                fontFamily = PeloFonts.Numbers,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                modifier = Modifier.alpha(timerAlpha)
            )
        }

        if (isMinimized) {
            MiniMetric(powerLabel, "W", MetricPowerColor)
            MiniMetric(cadenceLabel, "rpm", MetricCadenceColor)
            MiniMetric(resistanceLabel, "%", MetricResistanceColor)
            MiniMetric(heartRateLabel, "bpm", MetricHeartRateColor)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(
                size = SettingsButtonSize,
                background = PeloColors.Surface,
                onClick = onOpenSettings
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Open settings",
                    tint = PeloColors.Pumice,
                    modifier = Modifier.size(22.dp)
                )
            }
            RoundButton(
                size = ToggleButtonSize,
                background = PeloColors.Cardinal,
                onClick = onMinimizeToggle
            ) {
                Icon(
                    imageVector = if (isMinimized) {
                        when (location) {
                            OverlayLocation.Top -> Icons.Filled.KeyboardArrowDown
                            OverlayLocation.Bottom -> Icons.Filled.KeyboardArrowUp
                        }
                    } else {
                        when (location) {
                            OverlayLocation.Top -> Icons.Filled.KeyboardArrowUp
                            OverlayLocation.Bottom -> Icons.Filled.KeyboardArrowDown
                        }
                    },
                    contentDescription = if (isMinimized) "Expand" else "Minimize",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniMetric(value: String, unit: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            value,
            color = color,
            fontFamily = PeloFonts.Numbers,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            modifier = Modifier.alignByBaseline()
        )
        Text(
            unit,
            color = PeloColors.TextMuted,
            fontFamily = PeloFonts.Body,
            fontSize = 12.sp,
            modifier = Modifier.alignByBaseline()
        )
    }
}

@Composable
private fun RoundButton(
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(background, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
