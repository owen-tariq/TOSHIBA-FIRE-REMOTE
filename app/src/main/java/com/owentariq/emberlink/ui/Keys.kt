package com.owentariq.emberlink.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Press behaviour shared by every key on the remote.
 *
 * A plain tap fires once. Holding a key that opts into [repeat] keeps firing at
 * [REPEAT_INTERVAL_MS] after an initial [REPEAT_DELAY_MS] pause, which is what makes
 * scrolling a long Fire TV row bearable. Every fire gets a haptic tick, because you
 * are looking at the TV and not at the phone.
 */
private const val REPEAT_DELAY_MS = 400L

/**
 * A NEC frame occupies the transmitter for roughly 47 ms, so anything under that is
 * wasted queue pressure. 80 ms lands around six repeats a second — brisk enough to
 * cross a Fire TV row without overshooting every time.
 */
private const val REPEAT_INTERVAL_MS = 80L

@Composable
private fun Modifier.keyPress(
    enabled: Boolean,
    repeat: Boolean,
    onPressedChange: (Boolean) -> Unit,
    onFire: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    var held by remember { mutableStateOf(false) }

    if (repeat) {
        LaunchedEffect(held) {
            if (!held) return@LaunchedEffect
            delay(REPEAT_DELAY_MS)
            while (true) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFire()
                delay(REPEAT_INTERVAL_MS)
            }
        }
    }

    return this.pointerInput(enabled, repeat) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            held = true
            onPressedChange(true)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onFire()
            waitForUpOrCancellation()
            held = false
            onPressedChange(false)
        }
    }
}

/** A round key, the default shape on this remote. */
@Composable
fun RoundKey(
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    icon: ImageVector? = null,
    label: String? = null,
    contentDescription: String,
    enabled: Boolean = true,
    repeat: Boolean = false,
    face: Color = KeyFace,
    tint: Color = TextPrimary,
    onFire: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.90f else 1f, label = "keyScale")

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(if (pressed) KeyFaceHot else face, CircleShape)
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.06f else 0.02f), CircleShape)
            .keyPress(enabled, repeat, { pressed = it }, onFire),
        contentAlignment = Alignment.Center,
    ) {
        val alpha = if (enabled) 1f else 0.30f
        when {
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint.copy(alpha = alpha),
                modifier = Modifier.size(size * 0.42f),
            )

            label != null -> Text(
                text = label,
                color = tint.copy(alpha = alpha),
                fontSize = if (label.length > 3) 11.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A wide pill key, used for the app-launch row and utility actions. */
@Composable
fun PillKey(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean = true,
    face: Color = KeyFace,
    tint: Color = TextPrimary,
    height: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    onFire: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "pillScale")

    Box(
        modifier = modifier
            .scale(scale)
            .background(if (enabled) face else KeyFace, shape)
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .keyPress(enabled, repeat = false, onPressedChange = { pressed = it }, onFire = onFire),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = tint.copy(alpha = if (enabled) 1f else 0.30f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

/** Small caption used above groups of keys. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
    )
}
