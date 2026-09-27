package com.spop.poverlay.overlay

/** The live ride's state, shared with screens other than the overlay (e.g. the ride dashboard). */
data class RideSession(
    val sensor: OverlaySensorViewModel,
    val timer: OverlayTimerViewModel,
)
