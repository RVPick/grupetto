package com.spop.poverlay.launcher

import android.text.format.DateUtils
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spop.poverlay.ui.theme.PeloColors
import com.spop.poverlay.ui.theme.PeloFonts

private val CardShape = RoundedCornerShape(24.dp)
private val TileShape = RoundedCornerShape(20.dp)
private val ButtonShape = RoundedCornerShape(16.dp)
private val LiveGreen = Color(0xFF5BD69A)

data class HomeState(
    val greeting: String,
    val rideActive: Boolean,
    val bikeConnected: Boolean,
    val heartRateDevice: String?,
    val pinnedApps: List<LaunchableApp>,
    val allApps: List<LaunchableApp>,
    val pinnedPackages: Set<String>,
    val showAllApps: Boolean,
    val showRunningApps: Boolean = false,
    /** Apps opened since boot and not closed since; null when Pelo lacks usage access. */
    val runningApps: List<RunningApp>? = emptyList(),
    val freeMemoryMb: Long? = null,
    val pelotonLogo: ImageBitmap? = null,
)

class HomeActions(
    val onStartRide: () -> Unit,
    val onOpenDashboard: () -> Unit,
    val onEndRide: () -> Unit,
    val onOpenApp: (String) -> Unit,
    val onTogglePin: (String) -> Unit,
    val onShowAllApps: (Boolean) -> Unit,
    val onOpenOverlaySettings: () -> Unit,
    val onOpenSystemSettings: () -> Unit,
    val onOpenPeloton: () -> Unit,
    val onShowRunningApps: (Boolean) -> Unit,
    val onCloseApp: (String) -> Unit,
    val onCloseAllApps: () -> Unit,
    val onSwitchToApp: (String) -> Unit,
    val onUninstall: (String) -> Unit,
)

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PeloColors.Woodsmoke)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Header(state, actions)
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                JustRideCard(state.rideActive, actions)
                AppGrid(state, actions, Modifier.weight(1f))
            }
            BottomBar(state, actions)
        }
        if (state.showAllApps) {
            AllAppsSheet(state, actions)
        }
        if (state.showRunningApps) {
            RunningAppsSheet(state, actions)
        }
    }
}

@Composable
private fun Header(state: HomeState, actions: HomeActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (state.rideActive) "RIDE IN PROGRESS" else "READY TO RIDE",
                color = PeloColors.Text,
                fontFamily = PeloFonts.Numbers,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.5.sp
            )
            Text(state.greeting, color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 15.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CloseAppsButton { actions.onShowRunningApps(true) }
            StatusChip("Bike", state.bikeConnected)
            StatusChip(state.heartRateDevice ?: "No HR strap", state.heartRateDevice != null)
        }
    }
}

@Composable
private fun StatusChip(label: String, live: Boolean) {
    Chip(label) {
        Box(
            Modifier
                .size(8.dp)
                .background(if (live) LiveGreen else PeloColors.TextMuted, CircleShape)
        )
    }
}

/** Pill used for the header's status chips and the Close apps button, so they all match. */
@Composable
private fun Chip(label: String, onClick: (() -> Unit)? = null, leading: @Composable () -> Unit) {
    Row(
        Modifier
            .background(PeloColors.Surface, CircleShape)
            .border(1.dp, PeloColors.Divider, CircleShape)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        leading()
        Text(label, color = PeloColors.Pumice, fontFamily = PeloFonts.Body, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun CloseAppsButton(onClick: () -> Unit) {
    Chip("Close apps", onClick) {
        Icon(Icons.Filled.Close, contentDescription = null, tint = PeloColors.Pumice, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun JustRideCard(rideActive: Boolean, actions: HomeActions) {
    Column(
        Modifier
            .width(440.dp)
            .fillMaxHeight()
            .background(PeloColors.Surface, CardShape)
            .border(1.dp, PeloColors.Divider, CardShape)
            .padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "JUST RIDE",
                color = PeloColors.CardinalBright,
                fontFamily = PeloFonts.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
            Text(
                "Free ride,\nyour screen.",
                color = PeloColors.Text,
                fontFamily = PeloFonts.Numbers,
                fontWeight = FontWeight.SemiBold,
                fontSize = 56.sp,
                lineHeight = 54.sp
            )
            Text(
                if (rideActive) {
                    "Your metrics are on. Open an app and they stay on top."
                } else {
                    "Start the ride overlay, then open any app. Your metrics stay on top."
                },
                color = PeloColors.TextMuted,
                fontFamily = PeloFonts.Body,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (rideActive) {
                BigButton(
                    "Ride dashboard",
                    background = PeloColors.Cardinal,
                    content = Color.White,
                    onClick = actions.onOpenDashboard
                )
                BigButton(
                    "End ride",
                    background = PeloColors.Pumice,
                    content = PeloColors.Woodsmoke,
                    height = 52.dp,
                    onClick = actions.onEndRide
                )
            } else {
                BigButton(
                    "Start ride",
                    icon = Icons.Filled.PlayArrow,
                    background = PeloColors.Cardinal,
                    content = Color.White,
                    onClick = actions.onStartRide
                )
            }
        }
    }
}

@Composable
private fun BigButton(
    label: String,
    background: Color,
    content: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    height: Dp = 64.dp,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(background, ButtonShape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = content, fontFamily = PeloFonts.Body, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(state: HomeState, actions: HomeActions, modifier: Modifier) {
    val tiles: List<LaunchableApp?> = state.pinnedApps.take(AppRepository.MaxPinned).let {
        // A trailing null is the "All apps" tile when there's room for it.
        if (it.size < AppRepository.MaxPinned) it + null else it
    }
    Column(modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        tiles.chunked(3).let { rows -> rows + List(2 - rows.size) { emptyList() } }.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (i in 0 until 3) {
                    val cell = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    when {
                        i >= row.size -> Spacer(cell)
                        row[i] == null -> AllAppsTile(cell) { actions.onShowAllApps(true) }
                        else -> {
                            val app = row[i]!!
                            AppTile(
                                app = app,
                                note = if (state.rideActive) "Metrics on top" else "Hold to unpin",
                                modifier = cell.combinedClickable(
                                    onClick = { actions.onOpenApp(app.packageName) },
                                    onLongClick = { actions.onTogglePin(app.packageName) }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(app: LaunchableApp, note: String, modifier: Modifier) {
    Column(
        modifier
            .background(PeloColors.Surface, TileShape)
            .border(1.dp, PeloColors.Divider, TileShape)
            .padding(22.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Image(app.icon, contentDescription = null, modifier = Modifier.size(56.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                app.label,
                color = PeloColors.Text,
                fontFamily = PeloFonts.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(note, color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AllAppsTile(modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .border(1.dp, PeloColors.Divider, TileShape)
            .clickable(onClick = onClick)
            .padding(22.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(PeloColors.Surface, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.List, contentDescription = null, tint = PeloColors.Pumice, modifier = Modifier.size(28.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("All apps", color = PeloColors.Text, fontFamily = PeloFonts.Body, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text("Open or pin more", color = PeloColors.TextMuted, fontFamily = PeloFonts.Body, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BottomBar(state: HomeState, actions: HomeActions) {
    // The ride pill sits bottom-center, so keep buttons at the edges where it can't cover them.
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BarButton("All apps", Icons.Filled.List) { actions.onShowAllApps(true) }
            BarButton("Overlay", Icons.Filled.Build, onClick = actions.onOpenOverlaySettings)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BarButton("Settings", Icons.Filled.Settings, onClick = actions.onOpenSystemSettings)
            BarButton("Peloton", null, accent = true, logo = state.pelotonLogo, onClick = actions.onOpenPeloton)
        }
    }
}

@Composable
private fun BarButton(
    label: String,
    icon: ImageVector?,
    accent: Boolean = false,
    logo: ImageBitmap? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .width(172.dp)
            .fillMaxHeight()
            .border(1.dp, if (accent) PeloColors.Cardinal else PeloColors.Divider, ButtonShape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (logo != null) {
            Image(
                logo,
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = PeloColors.Pumice, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = if (accent) PeloColors.CardinalBright else PeloColors.Text,
            fontFamily = PeloFonts.Body,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun AllAppsSheet(state: HomeState, actions: HomeActions) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PeloColors.Woodsmoke)
            .clickable(enabled = false) {}
            .padding(horizontal = 40.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "ALL APPS",
                    color = PeloColors.Text,
                    fontFamily = PeloFonts.Numbers,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 40.sp
                )
                Text(
                    "Tap to open. Pin up to ${AppRepository.MaxPinned} for the home screen.",
                    color = PeloColors.TextMuted,
                    fontFamily = PeloFonts.Body,
                    fontSize = 15.sp
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .background(PeloColors.Surface, CircleShape)
                    .clickable { actions.onShowAllApps(false) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = PeloColors.Text, modifier = Modifier.size(28.dp))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.allApps, key = { it.packageName }) { app ->
                val pinned = app.packageName in state.pinnedPackages
                Column(
                    Modifier
                        .background(PeloColors.Surface, TileShape)
                        .border(1.dp, PeloColors.Divider, TileShape)
                        .clickable { actions.onOpenApp(app.packageName) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Image(app.icon, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.weight(1f))
                        if (app.canUninstall) {
                            Box(
                                Modifier
                                    .padding(end = 8.dp)
                                    .size(44.dp)
                                    .background(PeloColors.Woodsmoke, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { actions.onUninstall(app.packageName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Uninstall ${app.label}",
                                    tint = PeloColors.Pumice,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            if (pinned) "Pinned" else "Pin",
                            color = if (pinned) PeloColors.Text else PeloColors.Pumice,
                            fontFamily = PeloFonts.Body,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .background(if (pinned) PeloColors.Cardinal else PeloColors.Woodsmoke, CircleShape)
                                .clickable { actions.onTogglePin(app.packageName) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                    Text(
                        app.label,
                        color = PeloColors.Text,
                        fontFamily = PeloFonts.Body,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RunningAppsSheet(state: HomeState, actions: HomeActions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { actions.onShowRunningApps(false) }
            // Start below Peloton's banner, which always covers the top center of the screen.
            .padding(top = 124.dp, bottom = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .width(760.dp)
                .background(PeloColors.Surface, CardShape)
                .border(1.dp, PeloColors.Divider, CardShape)
                .clickable(enabled = false) {}
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "RUNNING APPS",
                        color = PeloColors.Text,
                        fontFamily = PeloFonts.Numbers,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 34.sp
                    )
                    Text(
                        state.freeMemoryMb?.let { "$it MB of memory free" } ?: " ",
                        color = PeloColors.TextMuted,
                        fontFamily = PeloFonts.Body,
                        fontSize = 15.sp
                    )
                }
                val canCloseAll = state.runningApps == null || state.runningApps.isNotEmpty()
                Box(
                    Modifier
                        .height(56.dp)
                        .background(if (canCloseAll) PeloColors.Cardinal else PeloColors.Woodsmoke, ButtonShape)
                        .clip(ButtonShape)
                        .clickable(enabled = canCloseAll, onClick = actions.onCloseAllApps)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Close all",
                        color = if (canCloseAll) Color.White else PeloColors.TextMuted,
                        fontFamily = PeloFonts.Body,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                }
                Box(
                    Modifier
                        .size(56.dp)
                        .background(PeloColors.Woodsmoke, CircleShape)
                        .clip(CircleShape)
                        .clickable { actions.onShowRunningApps(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = PeloColors.Text, modifier = Modifier.size(28.dp))
                }
            }

            val running = state.runningApps
            when {
                running == null -> Text(
                    "Pelo can't see which apps are running yet. Run scripts/grant.sh to allow usage access. Close all still works.",
                    color = PeloColors.Pumice,
                    fontFamily = PeloFonts.Body,
                    fontSize = 17.sp,
                    lineHeight = 24.sp
                )
                running.isEmpty() -> Text(
                    "No apps running.",
                    color = PeloColors.Pumice,
                    fontFamily = PeloFonts.Body,
                    fontSize = 17.sp
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(running, key = { it.app.packageName }) { item ->
                        RunningAppRow(item, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningAppRow(item: RunningApp, actions: HomeActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(PeloColors.Woodsmoke, TileShape)
            .border(1.dp, PeloColors.Divider, TileShape)
            .clip(TileShape)
            .clickable { actions.onSwitchToApp(item.app.packageName) }
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(item.app.icon, contentDescription = null, modifier = Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.app.label,
                color = PeloColors.Text,
                fontFamily = PeloFonts.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                lastUsedLabel(item.lastUsedMillis),
                color = PeloColors.TextMuted,
                fontFamily = PeloFonts.Body,
                fontSize = 13.sp
            )
        }
        Box(
            Modifier
                .height(56.dp)
                .width(120.dp)
                .border(1.dp, PeloColors.BorderStrong, ButtonShape)
                .clip(ButtonShape)
                .clickable { actions.onCloseApp(item.app.packageName) },
            contentAlignment = Alignment.Center
        ) {
            Text("Close", color = PeloColors.Text, fontFamily = PeloFonts.Body, fontWeight = FontWeight.Medium, fontSize = 17.sp)
        }
    }
}

private fun lastUsedLabel(lastUsedMillis: Long): String {
    val now = System.currentTimeMillis()
    return if (now - lastUsedMillis < DateUtils.MINUTE_IN_MILLIS) {
        "Used just now"
    } else {
        "Used " + DateUtils.getRelativeTimeSpanString(lastUsedMillis, now, DateUtils.MINUTE_IN_MILLIS)
    }
}
