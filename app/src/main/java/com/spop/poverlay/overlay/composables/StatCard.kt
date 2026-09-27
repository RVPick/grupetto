package com.spop.poverlay.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spop.poverlay.ui.theme.PeloColors
import com.spop.poverlay.ui.theme.PeloFonts

/**
 * One metric in the overlay bar: caps label, large number with unit, and a
 * muted line of session stats (e.g. "avg 84 · max 110").
 *
 * [selected] marks the metric currently drawn in the chart.
 */
@Composable
fun StatCard(
    name: String,
    value: String,
    unit: String,
    modifier: Modifier,
    detail: String? = null,
    color: Color = PeloColors.Text,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onUnitClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = name.uppercase(),
            color = if (selected) PeloColors.CardinalBright else PeloColors.TextMuted,
            fontFamily = PeloFonts.Body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                color = color,
                fontFamily = PeloFonts.Numbers,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 44.sp,
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = unit,
                color = PeloColors.TextMuted,
                fontFamily = PeloFonts.Body,
                fontSize = 13.sp,
                textDecoration = if (onUnitClick != null) TextDecoration.Underline else null,
                maxLines = 1,
                modifier = Modifier
                    .alignByBaseline()
                    .then(if (onUnitClick != null) Modifier.clickable { onUnitClick() } else Modifier)
            )
        }
        Text(
            text = detail ?: "",
            color = PeloColors.TextMuted,
            fontFamily = PeloFonts.Body,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
