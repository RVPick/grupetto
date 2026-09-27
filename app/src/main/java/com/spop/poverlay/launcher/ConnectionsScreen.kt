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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spop.poverlay.sensor.heartrate.HeartRateDevice
import com.spop.poverlay.ui.theme.PeloColors
import com.spop.poverlay.ui.theme.PeloFonts

private val CardShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(16.dp)
private val LiveGreen = Color(0xFF5BD69A)

data class AudioDevice(val name: String, val connected: Boolean)

data class ConnectionsState(
    val audioDevices: List<AudioDevice>,
    val heartRateDevice: HeartRateDevice?,
    val heartRate: Int?,
    val savedHeartRateDevices: List<HeartRateDevice>,
    val discoveredHeartRateDevices: List<HeartRateDevice>,
    val scanning: Boolean,
    val matchByName: Boolean,
    /** Where zones 2–5 start, in bpm; null when not set. */
    val zones: List<Int?>,
    val bleBroadcast: Boolean,
    val wifiBroadcast: Boolean,
    val broadcastName: String,
    val showTimerWhenMinimized: Boolean,
)

class ConnectionsActions(
    val onClose: () -> Unit,
    val onOpenBluetoothSettings: () -> Unit,
    val onConnectHeartRate: (HeartRateDevice) -> Unit,
    val onDisconnectHeartRate: () -> Unit,
    val onForgetHeartRate: (String) -> Unit,
    val onScan: () -> Unit,
    val onMatchByName: (Boolean) -> Unit,
    val onSaveZones: (List<Int?>) -> Unit,
    val onBleBroadcast: (Boolean) -> Unit,
    val onWifiBroadcast: (Boolean) -> Unit,
    val onShowTimer: (Boolean) -> Unit,
)

@Composable
fun ConnectionsScreen(state: ConnectionsState, actions: ConnectionsActions) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PeloColors.Woodsmoke)
            .padding(horizontal = 40.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "CONNECTIONS",
                    color = PeloColors.Text,
                    fontFamily = PeloFonts.Numbers,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 40.sp,
                    lineHeight = 40.sp
                )
                Text(
                    "Headphones, heart rate and sharing your ride with other apps",
                    color = PeloColors.TextMuted,
                    fontFamily = PeloFonts.Body,
                    fontSize = 15.sp
                )
            }
            RoundIconButton(onClick = actions.onClose)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                HeartRateCard(state, actions)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                HeadphonesCard(state, actions)
                BroadcastingCard(state, actions)
                OverlayCard(state, actions)
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PeloColors.Surface, CardShape)
            .border(1.dp, PeloColors.Divider, CardShape)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun SectionLabel(text: String, color: Color = PeloColors.TextMuted) {
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
private fun BodyText(text: String, color: Color = PeloColors.TextMuted) {
    Text(text, color = color, fontFamily = PeloFonts.Body, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun PillButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .height(48.dp)
            .background(if (primary) PeloColors.Cardinal else PeloColors.Woodsmoke, CircleShape)
            .border(1.dp, if (primary) PeloColors.Cardinal else PeloColors.BorderStrong, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = PeloColors.Text,
            fontFamily = PeloFonts.Body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .background(PeloColors.Surface, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PeloColors.Text, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun StatusDot(live: Boolean) {
    Box(
        Modifier
            .size(10.dp)
            .background(if (live) LiveGreen else PeloColors.TextMuted, CircleShape)
    )
}

/** A device row: status dot, name and detail, then trailing buttons. */
@Composable
private fun DeviceRow(
    name: String,
    detail: String?,
    live: Boolean,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PeloColors.Woodsmoke, RowShape)
            .border(1.dp, PeloColors.Divider, RowShape)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatusDot(live)
        Column(Modifier.weight(1f)) {
            Text(
                name,
                color = PeloColors.Text,
                fontFamily = PeloFonts.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detail != null) {
                Text(detail, color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { trailing() }
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PeloColors.Text, fontFamily = PeloFonts.Body, fontWeight = FontWeight.Medium, fontSize = 17.sp)
            BodyText(detail)
        }
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
}

@Composable
private fun HeartRateCard(state: ConnectionsState, actions: ConnectionsActions) {
    Card("HEART RATE") {
        val connected = state.heartRateDevice
        if (connected != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        state.heartRate?.toString() ?: "–",
                        color = PeloColors.Text,
                        fontFamily = PeloFonts.Numbers,
                        fontWeight = FontWeight.Bold,
                        fontSize = 64.sp,
                        lineHeight = 64.sp,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text("bpm", color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 17.sp, modifier = Modifier.alignByBaseline())
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        connected.name ?: connected.address,
                        color = PeloColors.Text,
                        fontFamily = PeloFonts.Body,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Connected", color = LiveGreen, fontFamily = PeloFonts.Body, fontSize = 14.sp)
                }
                PillButton("Disconnect", onClick = actions.onDisconnectHeartRate)
            }
        } else {
            Text(
                "Not connected",
                color = PeloColors.Pumice,
                fontFamily = PeloFonts.Body,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )
            BodyText("Start broadcasting on your strap or watch app (like HeartCast on your iPhone), then pick it below.")
        }

        if (state.savedHeartRateDevices.isNotEmpty()) {
            SectionLabel("YOUR DEVICES")
            state.savedHeartRateDevices.forEach { device ->
                val isConnected = device.address == connected?.address
                DeviceRow(
                    name = device.name ?: device.address,
                    detail = if (isConnected) "Connected" else "Reconnects automatically when it's broadcasting",
                    live = isConnected,
                ) {
                    if (!isConnected) PillButton("Connect", primary = true) { actions.onConnectHeartRate(device) }
                    PillButton("Forget") { actions.onForgetHeartRate(device.address) }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (state.scanning) "NEARBY · SEARCHING…" else "NEARBY", Modifier.weight(1f))
            PillButton(if (state.scanning) "Stop" else "Search", onClick = actions.onScan)
        }
        if (state.discoveredHeartRateDevices.isEmpty()) {
            BodyText(if (state.scanning) "Looking for heart rate monitors…" else "Tap Search to look for heart rate monitors.")
        } else {
            state.discoveredHeartRateDevices.forEach { device ->
                DeviceRow(name = device.name ?: device.address, detail = null, live = false) {
                    PillButton("Connect", primary = true) { actions.onConnectHeartRate(device) }
                }
            }
        }

        SwitchRow(
            title = "Match by name",
            detail = "Find your device by its name. Turn on for iPhones and Apple Watch apps, which change Bluetooth address.",
            checked = state.matchByName,
            onChange = actions.onMatchByName
        )

        ZonesEditor(state.zones, actions.onSaveZones)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier) {
    Box(modifier) { SectionLabel(text) }
}

@Composable
private fun ZonesEditor(zones: List<Int?>, onSave: (List<Int?>) -> Unit) {
    val fields = remember { mutableStateListOf("", "", "", "") }
    LaunchedEffect(zones) {
        zones.forEachIndexed { i, z -> if (fields[i].isBlank() && z != null) fields[i] = z.toString() }
    }
    SectionLabel("ZONES")
    BodyText("The bpm where each zone starts. Used for the zone bar on the ride dashboard.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        (0 until 4).forEach { i ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Zone ${i + 2}", color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 13.sp)
                BasicTextField(
                    value = fields[i],
                    onValueChange = { v -> fields[i] = v.filter { it.isDigit() }.take(3) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        color = PeloColors.Text,
                        fontFamily = PeloFonts.Numbers,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(PeloColors.CardinalBright),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(PeloColors.Woodsmoke, RowShape)
                        .border(1.dp, PeloColors.BorderStrong, RowShape)
                        .padding(top = 10.dp)
                )
            }
        }
        PillButton("Save", primary = true) { onSave(fields.map { it.toIntOrNull() }) }
    }
}

@Composable
private fun HeadphonesCard(state: ConnectionsState, actions: ConnectionsActions) {
    Card("HEADPHONES & SPEAKERS") {
        if (state.audioDevices.isEmpty()) {
            BodyText("No headphones or speakers paired yet.")
        } else {
            state.audioDevices.forEach { device ->
                DeviceRow(
                    name = device.name,
                    detail = if (device.connected) "Playing audio" else "Paired",
                    live = device.connected,
                ) {}
            }
        }
        BodyText("Pairing and switching happen in Android's Bluetooth settings. Once paired, all audio plays through them.")
        PillButton("Pair or switch in Bluetooth settings", onClick = actions.onOpenBluetoothSettings)
    }
}

@Composable
private fun BroadcastingCard(state: ConnectionsState, actions: ConnectionsActions) {
    Card("BROADCASTING") {
        BodyText("Share power, cadence and heart rate with other apps and devices, like Zwift or a Garmin.")
        SwitchRow(
            title = "Bluetooth",
            detail = "For watches, bike computers and apps on phones and tablets",
            checked = state.bleBroadcast,
            onChange = actions.onBleBroadcast
        )
        SwitchRow(
            title = "Wi-Fi (DIRCON)",
            detail = "For Zwift on a computer or Apple TV on the same network",
            checked = state.wifiBroadcast,
            onChange = actions.onWifiBroadcast
        )
        if (state.bleBroadcast || state.wifiBroadcast) {
            BodyText("Shows up as \"${state.broadcastName}\"", PeloColors.Pumice)
        }
    }
}

@Composable
private fun OverlayCard(state: ConnectionsState, actions: ConnectionsActions) {
    Card("OVERLAY") {
        SwitchRow(
            title = "Timer in compact mode",
            detail = "Show the ride timer in the small pill",
            checked = state.showTimerWhenMinimized,
            onChange = actions.onShowTimer
        )
    }
}
