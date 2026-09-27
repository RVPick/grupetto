package com.spop.poverlay.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.spop.poverlay.ConfigurationRepository
import com.spop.poverlay.overlay.OverlayLayout
import com.spop.poverlay.overlay.OverlayLayoutStore

/** Pelo's Overlay page: choose what the ride overlay shows and how it starts. */
class OverlayEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = ConfigurationRepository(applicationContext, this)

        setContent {
            val layout by OverlayLayoutStore.layout.collectAsState()
            val showTimer by configuration.showTimerWhenMinimized.collectAsState()
            OverlayEditorScreen(
                layout = layout,
                showTimerInPill = showTimer,
                onChange = { transform -> OverlayLayoutStore.update(transform) },
                onShowTimerInPill = configuration::setShowTimerWhenMinimized,
                onReset = {
                    OverlayLayoutStore.update { OverlayLayout() }
                    configuration.setShowTimerWhenMinimized(true)
                },
                onClose = ::finish,
            )
        }
    }
}
