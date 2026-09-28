package com.spop.poverlay.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Swallows taps (with no ripple) so they don't fall through to whatever is behind,
 * e.g. a dimmed background that closes a panel. A disabled clickable doesn't do this.
 */
fun Modifier.consumeTaps(): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
}
