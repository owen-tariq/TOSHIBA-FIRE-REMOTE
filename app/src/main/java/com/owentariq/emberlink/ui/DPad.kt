package com.owentariq.emberlink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The navigation ring, laid out to match the physical remote: a wide outer donut with
 * four arrows on the compass points and a raised OK disc in the middle.
 *
 * Arrows repeat while held. OK deliberately does not — nobody wants to select something
 * eleven times because their thumb lingered.
 */
@Composable
fun DPad(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onSelect: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(236.dp)
            .background(
                brush = Brush.verticalGradient(listOf(RingFace, Color(0xFF141519))),
                shape = CircleShape,
            )
            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        RoundKey(
            modifier = Modifier.align(Alignment.TopCenter),
            size = 66.dp,
            icon = Icons.Rounded.KeyboardArrowUp,
            contentDescription = "Up",
            enabled = enabled,
            repeat = true,
            face = Color.Transparent,
            onFire = onUp,
        )
        RoundKey(
            modifier = Modifier.align(Alignment.BottomCenter),
            size = 66.dp,
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Down",
            enabled = enabled,
            repeat = true,
            face = Color.Transparent,
            onFire = onDown,
        )
        RoundKey(
            modifier = Modifier.align(Alignment.CenterStart),
            size = 66.dp,
            icon = Icons.Rounded.KeyboardArrowLeft,
            contentDescription = "Left",
            enabled = enabled,
            repeat = true,
            face = Color.Transparent,
            onFire = onLeft,
        )
        RoundKey(
            modifier = Modifier.align(Alignment.CenterEnd),
            size = 66.dp,
            icon = Icons.Rounded.KeyboardArrowRight,
            contentDescription = "Right",
            enabled = enabled,
            repeat = true,
            face = Color.Transparent,
            onFire = onRight,
        )

        RoundKey(
            size = 104.dp,
            label = "OK",
            contentDescription = "OK, select",
            enabled = enabled,
            repeat = false,
            face = KeyFace,
            onFire = onSelect,
        )
    }
}
